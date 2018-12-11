package me.lycrus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Setter
@Getter
public class SolcStandardInput {
    private Utils utils = new Utils();
    private ObjectMapper om;
    private ObjectNode standardInput;
    //fileLocationSlashString => Sol
    private Map<String, Sol> sources;

    public SolcStandardInput() {
        this.om = new ObjectMapper();
        this.standardInput = this.om.createObjectNode();
        this.sources = new LinkedHashMap<>();
        this.om.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public static SolcStandardInput create(PackageManager _packageManager) {
        SolcStandardInput created = new SolcStandardInput();

        ObjectNode root = created.standardInput;

        root.put("language", "Solidity");
        root.putObject("sources");

        ObjectNode sources = root.putObject("settings");
        ObjectNode settings = root.putObject("settings");
        settings.put("evmVersion", "byzantium");
        ObjectNode optimizer = settings.putObject("optimizer");

        optimizer.put("enabled", false);
        optimizer.put("runs", 0);

        ArrayNode remappings = settings.putArray("remappings");
        _packageManager.getAllForSolcRemapping().forEach(
                s -> remappings.add(s)
        );

        //remappings.add("/=/");

        ObjectNode outputSelection = settings.putObject("outputSelection");

        ObjectNode everySingleFile = outputSelection.putObject("*");

        ArrayNode eachFile = everySingleFile.putArray("");
        eachFile.add("legacyAST").add("ast");
        ArrayNode eachContract = everySingleFile.putArray("*");
        eachContract.add("abi").add("evm.bytecode.object").add("evm.bytecode.sourceMap").add("evm.deployedBytecode.object").add("evm.deployedBytecode.sourceMap");


        return created;
    }

    public SolcStandardInput addSources(Set<Sol> files) {
        for (Sol file : files) {
            System.out.println("solcStandardInput add source for : " + file.fileLocation);

            /*String originalFilePath = file.getAbsolutePath();

            String osIndependentFilePath = utils.toIndependantPath(originalFilePath);

            String content = null;
            try {
                content = new String(Files.readAllBytes(new File(originalFilePath).toPath()), Charset.forName("UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }

            Source created = new Source(osIndependentFilePath, originalFilePath, content);

            getSources().put(osIndependentFilePath, created);*/

            sources.put(file.fileLocationSlashString, file);

            ObjectNode sourcesElement = (ObjectNode) getStandardInput().get("sources");
            ObjectNode solFile = sourcesElement.putObject(file.fileLocationSlashString);
            solFile.put("content", file.content);
        }
        return this;
    }

    public void writeToOutputStream(OutputStream outputStream) throws IOException {
        String r = this.om.writerWithDefaultPrettyPrinter().writeValueAsString(this.standardInput);//keep this for test view
        System.out.println(r);
        this.om.writeValue(outputStream, this.standardInput);
    }

    /*@Setter
    @Getter
    static class Source {
        private String osIndependentFilePath;
        private String originalFilePath;
        private String content;

        public Source(String osIndependentFilePath, String originalFilePath, String content) {
            this.osIndependentFilePath = osIndependentFilePath;
            this.originalFilePath = originalFilePath;
            this.content = content;
        }


    }*/
}
