package me.lycrus.antlr;

import me.lycrus.PackageManager;
import me.lycrus.Sol;
import lombok.AllArgsConstructor;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.HashSet;
import java.util.Set;

import static me.lycrus.antlr.SolidityParser.ImportDirectiveContext;

@AllArgsConstructor
public class ImportsRetriever {

    public PackageManager packageManager;

    public Set<Sol> getImports(Sol sol) {
        CharStream charStream = CharStreams.fromString(sol.getContent());
        if (charStream == null) {
            throw new RuntimeException("read sol file fails");
        }
        SolidityParser parser = new SolidityParser(new CommonTokenStream(new SolidityLexer(charStream)));
        Set<Sol> files = new HashSet<>();
        for (ImportDirectiveContext importDirectiveContext : parser.sourceUnit().importDirective()) {
            String filename = importDirectiveContext.StringLiteral().getText();
            filename = filename.substring(1, filename.length() - 1).trim();//get rid of double quotation marks

            //now change relative path, package leading path to absolute path
            Sol needImport = packageManager.resolve(sol.fileLocation, filename);

            files.add(needImport);
        }
        return files;
    }
}

/*
import "filename";
import * as symbolName from "filename";
import {symbol1 as alias, symbol2} from "filename";
import "filename" as symbolName; = import * as symbolName from "filename";
 */