package de.strifel.vbans;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;

class JDBC {

    static void inject(VBans vBans) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            if (!new File("plugins/vbans/jdbc.jar").exists()) download();
            vBans.getServer().getPluginManager().addToClasspath(vBans, Paths.get(new File("plugins/vbans/jdbc.jar").getAbsolutePath()));
        }
    }

    private static void download() {
        File tmp = new File("tmp_vbans/");
        tmp.mkdir();
        Path zip = Paths.get(tmp.getAbsolutePath(), "jdbc.zip");
        try {
            URL jdbc = new URL("https://cdn.mysql.com//Downloads/Connector-J/mysql-connector-java-8.0.18.zip");
            try (InputStream in = jdbc.openStream()) {
                Files.copy(in, zip, StandardCopyOption.REPLACE_EXISTING);
            }
            try (FileSystem zipFileSystem = FileSystems.newFileSystem(zip, null)) {
                Files.copy(zipFileSystem.getPath("mysql-connector-java-8.0.18/mysql-connector-java-8.0.18.jar"), Paths.get(new File("plugins/vbans").getAbsolutePath(), "jdbc.jar"), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Cant download JDBC!");
        }
        tmp.delete();
    }

}
