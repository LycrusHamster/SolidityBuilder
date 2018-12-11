package me.lycrus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@AllArgsConstructor
public class TruffleContractWriter {
    public List<Path> outputPath;// mustn't contains null;

    public Set<SmartContract> write(SolcStandardOutput solcStandardOutput) {
        Map<SmartContract, ObjectNode> contracts = solcStandardOutput.getTruffleContracts();
        Iterator<Map.Entry<SmartContract, ObjectNode>> it = contracts.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<SmartContract, ObjectNode> e = it.next();

            for (Path path : outputPath) {
                e.getKey().jsonFile = writeIt(path, e.getKey().file.fullPackageName, e.getKey().contractName, e.getValue());
            }
        }

        return contracts.keySet();
    }

    private Path writeIt(Path outputDir, Path packageName, String contractName, ObjectNode value) {
        if (null == outputDir) {
            return null;
        }
        Path outPath = outputDir;
        /*for (String p : packageName) {
            outPath = outPath.resolve(p);
        }*/
        if(packageName != null){
            outPath = outPath.resolve(packageName);
        }
        outPath = outPath.resolve(contractName + ".json");
        File out = outPath.toFile();

        if (out.exists()) {
            out.delete();
        }
        try {
            FileUtils.forceMkdirParent(out);
            ObjectMapper om = new ObjectMapper();
            om.writerWithDefaultPrettyPrinter().writeValue(out, value);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outPath;
    }

}
