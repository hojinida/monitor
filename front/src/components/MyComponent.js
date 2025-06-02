import React, { useState, useEffect } from "react";
import styled from "styled-components";

function MyComponent() {
  const [namespaces, setNamespaces] = useState([]);
  const [deployments, setDeployments] = useState({});
  const [loading, setLoading] = useState(false);

  const handleReplicaUpdate = (namespace, deploymentName) => {
    const newReplicaCount = prompt("새로운 레플리카 수를 입력하세요:");
    if (newReplicaCount && !isNaN(newReplicaCount)) {
      setLoading(true); // 로딩 시작
      fetch("http://localhost:8080/api/deployments/scale", {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          namespace: namespace,
          name: deploymentName,
          replicas: newReplicaCount,
        }),
      })
        .then((response) => {
          if (!response.ok) {
            return response.json().then((errorData) => {
              throw new Error(errorData.message || "Unknown error");
            });
          }
          alert("레플리카 업데이트 성공!");
          // 성공 시 데이터 새로고침 또는 SSE를 통해 업데이트되므로 별도 fetch는 생략 가능
        })
        .catch((error) => {
          alert(`레플리카 업데이트 실패: ${error.message}`);
        })
        .finally(() => {
          setLoading(false); // 로딩 종료
        });
    } else if (newReplicaCount !== null) { // 사용자가 취소하지 않고 유효하지 않은 값을 입력한 경우
      alert("유효한 숫자를 입력하세요.");
    }
  };

  const handleImageUpdate = async (namespace, deploymentName, currentImage) => {
    const imageTag = prompt(
      `새로운 이미지 태그를 입력하세요 (예: 1.26.2)\n현재 이미지: ${currentImage}\n빈 문자열("")도 허용됩니다.`
    );

    if (imageTag === null) {
      // alert("이미지 태그 입력이 취소되었습니다."); // 사용자가 취소한 경우 조용히 종료
      return;
    }

    setLoading(true);

    try {
      const response = await fetch("http://localhost:8080/api/deployments/image", {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          namespace: namespace,
          name: deploymentName,
          imageTag: imageTag,
        }),
      });

      if (response.ok) {
        alert("이미지 업데이트 성공!");
        // 성공 시 데이터 새로고침 또는 SSE를 통해 업데이트되므로 별도 fetch는 생략 가능
      } else {
        const errorData = await response.json();
        alert(`이미지 업데이트 실패: ${errorData.message || "Unknown error"}`);
      }
    } catch (error) {
      alert("서버와의 통신 중 오류가 발생했습니다.");
      console.error("Error:", error);
    } finally {
      setLoading(false);
    }
  };

  // 1) 최초로 모든 네임스페이스 & 디플로이먼트 불러오기
  useEffect(() => {
    const fetchAllData = async () => {
      setLoading(true);
      try {
        const nsResponse = await fetch("http://localhost:8080/api/namespaces");
        if (!nsResponse.ok) throw new Error('Failed to fetch namespaces');
        const nsData = await nsResponse.json();
        setNamespaces(nsData);
        localStorage.setItem("namespaces", JSON.stringify(nsData));

        const deploymentPromises = nsData.map(async (nsItem) => {
          try {
            const depResponse = await fetch(
              `http://localhost:8080/api/deployments/${nsItem.name}`
            );
            if (!depResponse.ok) throw new Error(`Failed to fetch deployments for ${nsItem.name}`);
            const depData = await depResponse.json();
            localStorage.setItem(
              `deployments_${nsItem.name}`,
              JSON.stringify(depData)
            );
            return { [nsItem.name]: depData };
          } catch (depError) {
            console.error(`Error fetching deployments for ${nsItem.name}:`, depError);
            const storedData = localStorage.getItem(`deployments_${nsItem.name}`);
            return { [nsItem.name]: storedData ? JSON.parse(storedData) : [] };
          }
        });
        
        const deploymentsResults = await Promise.all(deploymentPromises);
        const combinedDeployments = deploymentsResults.reduce((acc, current) => ({ ...acc, ...current }), {});
        setDeployments(combinedDeployments);

      } catch (error) {
        console.error("Error fetching initial data:", error);
        // Fallback to localStorage if API fails
        const storedNs = localStorage.getItem("namespaces");
        if (storedNs) {
          const parsedNs = JSON.parse(storedNs);
          setNamespaces(parsedNs);
          const deploymentData = {};
          parsedNs.forEach((nsItem) => {
            const storedData = localStorage.getItem(`deployments_${nsItem.name}`);
            if (storedData) {
              deploymentData[nsItem.name] = JSON.parse(storedData);
            }
          });
          setDeployments(deploymentData);
        }
        alert("데이터 로딩 중 오류 발생. 로컬 스토리지 데이터를 사용합니다 (가능한 경우).");
      } finally {
        setLoading(false);
      }
    };

    fetchAllData();
  }, []);

  useEffect(() => {
    const eventSource = new EventSource("http://localhost:8080/api/sse/stream");

    eventSource.addEventListener("namespaceUpdate", (event) => {
      const data = JSON.parse(event.data);
      console.log("Received SSE for namespaceUpdate:", data);
      setNamespaces((prev) => {
        const updatedList = prev.map(ns => ns.name === data.name ? {...ns, ...data} : ns);
        if (!updatedList.find(ns => ns.name === data.name)) {
          updatedList.push(data);
        }
        localStorage.setItem("namespaces", JSON.stringify(updatedList));
        return updatedList;
      });
    });

    eventSource.addEventListener("deploymentUpdate", (event) => {
      const data = JSON.parse(event.data);
      console.log("Received SSE data for deploymentUpdate:", data);
      setDeployments((prev) => {
        const namespaceDeployments = prev[data.namespace] || [];
        let updatedDeployments = namespaceDeployments.map(dep => 
          dep.name === data.name ? { ...dep, ...data } : dep
        );
        if (!updatedDeployments.find(dep => dep.name === data.name)) {
          updatedDeployments.push(data);
        }
        localStorage.setItem(`deployments_${data.namespace}`, JSON.stringify(updatedDeployments));
        return { ...prev, [data.namespace]: updatedDeployments };
      });
    });

    eventSource.onerror = (error) => {
      console.log("EventSource failed:", error);
      // Consider implementing reconnection logic or user notification here
      eventSource.close();
    };

    return () => {
      eventSource.close();
    };
  }, []);


  return (
    <Container>
      <Navbar>
        <NavItem>
          <strong>K8S 모니터</strong>
        </NavItem>
        <NavItem>
          <strong>장호진</strong>
        </NavItem>
      </Navbar>
  
      <Title>
        <strong>Namespaces</strong>
      </Title>
  
      {loading && (
        <LoadingOverlay>
          <div className="spinner"></div>
          <p>작업 진행 중...</p>
        </LoadingOverlay>
      )}
  
      <ListContainer>
        {namespaces.map((nsItem, index) => {
          const depList = deployments[nsItem.name] || [];
  
          return (
            <NamespaceWrapper key={nsItem.name || index}>
              <NamespaceHeader>
                <NamespaceTitle>{nsItem.name}</NamespaceTitle>
                <NamespaceMeta>
                  <StatusIndicator status={nsItem.status}>
                    {nsItem.status === "ACTIVE" ? "🟢 ACTIVE" : "🔴 TERMINATING"}
                  </StatusIndicator>
                  <AgeText>age: {nsItem.age}</AgeText>
                </NamespaceMeta>
              </NamespaceHeader>
  
              <DeploymentsGrid>
                {depList.length === 0 ? (
                  <NoItemsMessage>No Deployments Found</NoItemsMessage>
                ) : (
                  depList.map((deployment, idx) => (
                    <DeploymentCard key={idx}>
                      <DeploymentTitle>{deployment.name}</DeploymentTitle>
                      <StatusIndicator status={deployment.status}>
                        {deployment.status === "RUNNING" ? "🟢 RUNNING"
                          : deployment.status === "FAILED" ? "🔴 FAILED"
                            : "🟡 PENDING"}
                      </StatusIndicator>
  
                      <DetailItem>
                        <DetailLabel>Image:</DetailLabel>
                        <ImageName>
                          {deployment.imageName}
                          {deployment.imageTag ? `:${deployment.imageTag}` : ''}
                        </ImageName>
                        <StyledButtonSmall
                          onClick={() => handleImageUpdate(nsItem.name, deployment.name, `${deployment.imageName}${deployment.imageTag ? `:${deployment.imageTag}` : ''}`)}
                        >
                          Update
                        </StyledButtonSmall>
                      </DetailItem>
  
                      <DetailItem>
                        <DetailLabel>Replicas:</DetailLabel>
                        <span>
                          {deployment.availableReplicas}/{deployment.desiredReplicas}
                        </span>
                        <StyledButtonSmall onClick={() => handleReplicaUpdate(nsItem.name, deployment.name)}>
                          Update
                        </StyledButtonSmall>
                      </DetailItem>
                    </DeploymentCard>
                  ))
                )}
              </DeploymentsGrid>
            </NamespaceWrapper>
          );
        })}
      </ListContainer>
    </Container>
  );
}

// -------------------
// styled-components
// -------------------
const Container = styled.div`
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  background-color: #f4f6f8; /* 밝은 회색 배경 */
  padding: 0;
  min-height: 100vh;
  color: #333;
`;

const Navbar = styled.div`
  background-color: #2c3e50; /* 어두운 남색 계열 */
  color: white;
  padding: 12px 25px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
`;

const NavItem = styled.div`
  margin-right: 20px;
  padding: 8px 12px;
  cursor: pointer;
  border-radius: 4px;
  transition: background-color 0.2s ease-in-out;
  &:hover {
    background-color: #34495e; /* 호버 시 약간 더 어두운 색 */
  }
`;

const Title = styled.h1` /* 시맨틱 태그 사용 */
  color: #2c3e50; /* Navbar와 일관된 어두운 색상 */
  font-size: 28px;
  padding: 20px 25px;
  margin: 0;
  font-weight: 600;
`;

const ListContainer = styled.div`
  padding: 0 25px 25px; /* Navbar, Title과 동일한 좌우 패딩 */
`;

const NamespaceWrapper = styled.div`
  background-color: #ffffff; /* 흰색 배경 */
  border-radius: 8px; /* 부드러운 모서리 */
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08); /* 은은한 그림자 효과 */
  margin-bottom: 25px;
  padding: 20px;
  transition: box-shadow 0.3s ease;

  &:hover {
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
  }
`;

const NamespaceHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e0e0e0; /* 구분선 */
`;

const NamespaceTitle = styled.h2` /* 시맨틱 태그 사용 */
  font-size: 22px;
  font-weight: 600;
  color: #34495e; /* 어두운 남색 계열 */
  margin: 0;
`;

const NamespaceMeta = styled.div`
  display: flex;
  align-items: center;
  gap: 15px;
`;

const DeploymentsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); /* 반응형 그리드 */
  gap: 20px; /* 카드 사이 간격 */
`;

const DeploymentCard = styled.div`
  background-color: #f9fafb; /* 매우 밝은 회색 배경 */
  border: 1px solid #e0e0e0; /* 연한 테두리 */
  border-radius: 6px;
  padding: 15px;
  display: flex;
  flex-direction: column;
  gap: 12px; /* 내부 아이템 간격 */
  box-shadow: 0 2px 5px rgba(0,0,0,0.05);
`;

const DeploymentTitle = styled.h3` /* 시맨틱 태그 사용 */
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 5px 0;
`;

const StatusIndicator = styled.div`
  display: flex;
  align-items: center;
  gap: 6px; /* 아이콘과 텍스트 간격 */
  font-weight: 500;
  font-size: 14px;
  color: ${({ status }) =>
    status === "ACTIVE" || status === "RUNNING" ? "#27ae60"  /* 초록색 (약간 톤 다운) */
    : status === "TERMINATING" || status === "FAILED" ? "#e74c3c"  /* 빨간색 (약간 톤 다운) */
    : status === "PENDING" ? "#f39c12"  /* 노란색 (약간 톤 다운) */
    : "#7f8c8d"}; /* 기본 회색 */
`;

const AgeText = styled.span`
  font-size: 14px;
  color: #7f8c8d; /* 연한 회색 */
  font-weight: 500;
`;

const DetailItem = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between; /* 레이블과 값, 버튼을 양쪽으로 분산 */
  gap: 10px;
  font-size: 14px;
  padding: 8px 0;
  border-bottom: 1px dashed #eee; /* 항목 간 옅은 구분선 */

  &:last-child {
    border-bottom: none;
  }
`;

const DetailLabel = styled.strong`
  color: #555;
  min-width: 60px; /* 레이블 너비 고정으로 정렬 용이 */
`;

const ImageName = styled.span`
  word-break: break-all; /* 긴 이미지 이름 줄바꿈 처리 */
  flex-grow: 1; /* 버튼 제외한 나머지 공간 차지 */
  margin-right: 10px; /* 버튼과의 간격 */
`;

const StyledButtonSmall = styled.button`
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  background-color: #3498db; /* 파란색 계열 버튼 */
  color: white;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s ease;
  white-space: nowrap; /* 버튼 텍스트 줄바꿈 방지 */

  &:hover {
    background-color: #2980b9; /* 호버 시 약간 더 어두운 파란색 */
  }
`;

const NoItemsMessage = styled.div`
  padding: 20px;
  text-align: center;
  color: #7f8c8d; /* 연한 회색 */
  font-style: italic;
  grid-column: 1 / -1; /* 그리드 전체 너비 차지 */
`;

const LoadingOverlay = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.6); /* 약간 더 어둡게 */
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
  color: white;
  font-size: 18px;
  font-weight: 500;
  z-index: 1000;

  .spinner {
    border: 4px solid rgba(255, 255, 255, 0.3);
    border-top: 4px solid #fff;
    border-radius: 50%;
    width: 40px;
    height: 40px;
    animation: spin 0.8s linear infinite;
    margin-bottom: 15px;
  }

  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }
`;

export default MyComponent;