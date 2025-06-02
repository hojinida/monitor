package com.example.monitor

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class MonitorApplication

fun main(args: Array<String>) {
	runApplication<MonitorApplication>(*args)
}
