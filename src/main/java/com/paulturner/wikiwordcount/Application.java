package com.paulturner.wikiwordcount;

import com.paulturner.wikiwordcount.cli.CommandLineOptionsExtractor;
import com.paulturner.wikiwordcount.cli.RuntimeOptions;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application implements CommandLineRunner, ApplicationContextAware {

    private static RuntimeOptions runtimeOptions;
    private ApplicationContext applicationContext;
    @Autowired
    private FileProcessor fileProcessor;

    public static void main(String[] args) {
        runtimeOptions = CommandLineOptionsExtractor.parse(args).get();
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public RuntimeOptions runtimeOptions() {
        return runtimeOptions;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) {
        fileProcessor.process();


    }

}
