package com.besafx.app;

import com.besafx.app.config.TwilioManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.ExecutionException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MainTests {

    private final static Logger log = LoggerFactory.getLogger(MainTests.class);

    @Autowired
    private TwilioManager twilioManager;

    @Test
    public void contextLoads() throws ExecutionException, InterruptedException {
        twilioManager.getMessages().stream().forEach(message -> log.info(message.toString()));
    }
}
