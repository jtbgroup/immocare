package com.immocare;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * To execute:
 * 
 * cd backend
 * mvn compile
 * java -cp target/classes:$(mvn -q dependency:build-classpath
 * -Dmdep.outputFile=/dev/stdout) HashGenerator
 * 
 */

public class HashGenerator {

    public static void main(String[] args) {
        String password = "Admin1234!";
        String hash = new BCryptPasswordEncoder(10).encode(password);
        System.out.println("Password : " + password);
        System.out.println("Hash     : " + hash);
    }
}