package com.knowledge.base.file;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * File service bootstrap class
 *
 * @author airwzz999
 * @since 2026-04-24
 */
@SpringBootApplication(scanBasePackages = "com.knowledge.base")
@MapperScan("com.knowledge.base.file.mapper")
public class FileApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileApplication.class, args);
        System.out.println("""

            ========================================
            /\\\\
              \\\\  /  \\\\
               \\\\/
              /  \\\\
             /    \\\\
            /      \\\\
           /        \\\\
          /          \\\\
         /            \\\\
        /              \\\\
       /                \\\\
      /                  \\\\
     /                    \\\\
    /                      \\\\
   /                        \\\\
  /                          \\\\
 /                            \\\\
=======================================
  File service started successfully!
  Port: 8084
  API docs: http://localhost:8084/api/file/doc.html
=======================================
""");
    }
}
