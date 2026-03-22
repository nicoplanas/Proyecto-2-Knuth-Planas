package com.unimet.so.proyecto2.persistence;

public class TestScenario {
    public String testId;
    public int initialHead;
    public FileSeed[] systemFiles;
    public Request[] requests;

    public static class FileSeed {
        public int position;
        public String name;
        public int blocks;
    }

    public static class Request {
        public int pos;
        public String op;
    }
}