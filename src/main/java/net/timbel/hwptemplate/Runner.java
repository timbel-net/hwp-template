package net.timbel.hwptemplate;

import com.google.gson.Gson;
import kr.dogfoot.hwplib.reader.HWPReader;
import lombok.val;
import lombok.var;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.exists;

public class Runner {

    public static void main(String... args) throws Exception {
        if (args.length != 3) {
            System.out.println("Usage: java -jar hwp-template.jar <template.hwp> <data.json> <output.hwp>");
            System.exit(1);
        }

        final Path template = toActualPath(args[0]);
        final Path jsonfile = toActualPath(args[1]);
        final Path output = toOutputPath(args[2]);

        val gson = new Gson();
        val data = gson.fromJson(Files.newBufferedReader(jsonfile), List.class);

        deleteIfExists(output);

        val tpl = new HwpTemplate(HWPReader.fromFile(template.toAbsolutePath().toString()));
        tpl.write(data);
        tpl.toStream(Files.newOutputStream(output));
    }

    private static Path toActualPath(String _path) {
        var path = Paths.get(_path);

        if (exists(path)) return path;
        else {
            path = Paths.get("").toAbsolutePath().resolve(_path);
            if (exists(path)) return path;
        }

        throw new IllegalArgumentException("File not found: " + _path);
    }

    private static Path toOutputPath(String output) {
        val path = Paths.get(output);

        if (exists(path)) return path;
        else {
            return Paths.get("").toAbsolutePath().resolve(output);
        }
    }
}
