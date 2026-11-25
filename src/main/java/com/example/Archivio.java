package com.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class Archivio extends Thread {
    private Socket s;
    // private String[] extensions = { "txt", "md", "csv" };
    private String[][] TIPI = {
                                {"txt", "text/plain; charset=utf-8"},
                                {"md",  "text/markdown; charset=utf-8"},
                                {"csv", "text/csv; charset=utf-8"},
                            };

    public Archivio(Socket s) {
        this.s = s;
    }

    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8),
                    true);

            boolean connection = true;
            while (connection) {
                String cmd = in.readLine();
                if(cmd == null) s.close();

                String[] parts = cmd.trim().split("\\s+");
                
                if (cmd.isEmpty() || parts.length != 2 || !validateName(parts[1])) {
                    out.println("ERR SYNTAX");
                } else if (!parts[0].equals("LEGGI")) {
                    out.println("ERR UNKNOWNCMD");
                } else {
                    File cartella = new File("documenti");
                    Object[] res = validateExtension(cartella, parts[1]);

                    if (res != null) {
                        if(res[0].equals("EXTERR")){
                            out.println("ERR SYNTAX");
                        }else{
                            File file = (File) res[0];
                            System.out.println(file.getAbsolutePath()); 
                            try (BufferedReader lettoreFile = new BufferedReader(
                                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                                
                                byte[] dati = Files.readAllBytes(file.toPath());
                                String contenuto = new String(dati, StandardCharsets.UTF_8);

                                out.println("OK FILE"); connection = false;
                                
                                out.println("Content-Type: " + (String) res[1]); 
                                out.println("Content-Length: " + dati.length);
                                out.println(); // Riga vuota
                                out.print(contenuto);
                                out.flush();

                            } catch (IOException e) {
                                out.println("ERR READ");
                                e.printStackTrace();
                                // Gestite l'errore distinguendo se lo stato è già stato inviato.
                            }
                        }
                    } else {
                        out.println("ERR NOTFOUND");
                    }
                }
            }
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean validateName(String filename) {
        String[] notallow = { "\\", "/", "..", " " };
        for (String c : notallow) {
            if (filename.contains(c)) return false;
        }
        return true;
    }

    public Object[] validateExtension(File cartella, String filename) {
        for(String[] e : TIPI){
            File file = new File(cartella, filename + "." + e[0]);
            if(file.isFile()) return new Object[]{file, e[1]};
        }
        File file = new File(cartella, filename);
        if(file.isFile()) return new Object[]{"EXTERR"};

        return null;
    }
}
