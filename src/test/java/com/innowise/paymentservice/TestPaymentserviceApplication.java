package com.innowise.paymentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class TestPaymentserviceApplication {

	public static void main(String[] args) {
		SpringApplication.from(PaymentserviceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
