package ru.netology;

import java.io.IOException;

public class Main {

    final static int portNumber = 9999;

    public static void main(String[] args) throws IOException {

        new Server(portNumber).connectServer();

    }

}



