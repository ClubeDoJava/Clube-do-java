package br.com.clubedojava.webstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching
@EnableAsync
public class ClubedoJavaApplication {

    public static void main(String[] args) {

        System.setProperty("co.paralleluniverse.fibers.detectRunawayFibers", "false");
        System.setProperty("java.util.concurrent.VirtualThreadFactory", "enabled");

        SpringApplication.run(ClubedoJavaApplication.class, args);
    }
}
