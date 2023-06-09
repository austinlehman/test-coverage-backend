package com.groupfour.testcoveragetool.controller;

import com.groupfour.testcoveragetool.group.gatling.GatlingEndpointEnumerator;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.lingala.zip4j.exception.ZipException;

@CrossOrigin(origins = "http://localhost:3000", methods = {RequestMethod.GET,
                                                            RequestMethod.POST,
                                                            RequestMethod.PUT,
                                                            RequestMethod.DELETE,
                                                            RequestMethod.PATCH},
    allowedHeaders = "*")
@RestController
@RequestMapping("/tests/gatling")
public class GatlingController {

    ConcurrentMap<String, Extractable> extractables = new ConcurrentHashMap<>();

    @PostMapping("/getAll")
    public List<EndpointInfo> getAll(@RequestParam("file") MultipartFile file) throws IOException, ZipException {

        File tempFile = File.createTempFile("temp-", file.getOriginalFilename());
        file.transferTo(tempFile);

        List<EndpointInfo> list = new ArrayList<>(GatlingEndpointEnumerator.listApiAnnotations(tempFile));
        String toRet = "";
        for(EndpointInfo e:list) {
            String path =  e.getPath().substring(1, e.getPath().length() - 1);
            toRet += e.getMethod() + " ";
            toRet += e.getPath() + '\n';
        }
        return list;
    }
}
