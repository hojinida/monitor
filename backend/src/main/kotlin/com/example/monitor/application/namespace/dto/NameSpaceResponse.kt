package com.example.monitor.application.namespace.dto

import com.example.monitor.application.util.AgeCalculator
import com.example.monitor.domain.namespace.NameSpace

class NameSpaceResponse(
    val name: String, val status: String, val age: String
) {
    companion object {
        fun fromDomain(ns: NameSpace): NameSpaceResponse {
            return NameSpaceResponse(
                name = ns.name, status = ns.status.name, age = AgeCalculator.computeAgeString(ns.creationTimestamp)
            )
        }
    }
}
