package com.example.tappe.tappa2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Archivio extends Thread {
    private Socket s;
    private String[] extensions = { "txt", "md", "csv" };

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
                        File file = (File) res[0];
                        System.out.println(file.getAbsolutePath()); 
                        try (BufferedReader lettoreFile = new BufferedReader(
                                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                            out.println("OK FILE"); connection = false;
                            out.println("TYPE " + (String) res[1]); 
                            out.println(""); // Riga vuota

                            String riga;
                            while ((riga = lettoreFile.readLine()) != null) {
                                out.println(riga);
                            }
                        } catch (IOException e) {
                            out.println("ERR READ");
                            e.printStackTrace();
                            // Gestite l'errore distinguendo se lo stato è già stato inviato.
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
        String[] notallow = { "\\", "/", "..", ".", " " };
        for (String c : notallow) {
            if (filename.contains(c))
                return false;
        }
        return true;
    }

    public Object[] validateExtension(File cartella, String filename) {
        for(String e : extensions){
            File file = new File(cartella, filename + "." + e);
            if(file.isFile()) return new Object[]{file, e};
        }
        return null;
    }
}
