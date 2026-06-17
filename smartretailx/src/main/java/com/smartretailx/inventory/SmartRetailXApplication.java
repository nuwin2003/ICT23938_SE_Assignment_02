package com.smartretailx.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SmartRetailX Inventory Management Service
 * Entry point for the Spring Boot application.
 *
 * Feature: Inventory Management with Low Stock Alert
 * This module handles product stock tracking, updates,
 * and automated low-stock notifications for the SmartRetailX platform.
 */
@SpringBootApplication
public class SmartRetailXApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartRetailXApplication.class, args);
    }
}
