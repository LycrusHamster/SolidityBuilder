package me.lycrus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class SolcStandardOutput {
    private PackageManager packageManager;
    private SolcStandardInput solcStandardInput;
    private ObjectNode standardOutput;
    private ObjectMapper om;
    private boolean compileResult;
    private List<Error> errors;
    private Map<SmartContract, ObjectNode> truffleContracts;

    public ObjectMapper getOm() {
        return this.om;
    }

    public boolean isCompileResult() {
        return this.compileResult;
    }

    public List<Error> getErrors() {
        return this.errors;
    }

    public Map<SmartContract, ObjectNode> getTruffleContracts() {
        return this.truffleContracts;
    }

    public SolcStandardOutput() {
        this.standardOutput = null;
        this.om = new ObjectMapper();
        this.compileResult = true;
        this.errors = new ArrayList<>();
        this.truffleContracts = new LinkedHashMap<>();
        this.om.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
    }

    public static SolcStandardOutput create(java.io.InputStream _is, SolcStandardInput _solcStandardInput, PackageManager _PackageManager) {
        SolcStandardOutput self = new SolcStandardOutput();
        self.setSolcStandardInput(_solcStandardInput);
        self.setPackageManager(_PackageManager);
        try {
            self.standardOutput = ((ObjectNode) self.om.readTree(_is));
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }


        //error check
        if (self.standardOutput.has("errors")) {
            boolean errorFound = false;
            ArrayNode errorArray = (ArrayNode) self.standardOutput.get("errors");
            for (JsonNode e : errorArray) {

                ObjectNode error = (ObjectNode) e;
                ObjectNode sourceLocationNode = (ObjectNode) error.get("sourceLocation");

                String _sourceLocation = "";
                if (sourceLocationNode != null) {
                    _sourceLocation = sourceLocationNode.get("file").asText() + ":" + sourceLocationNode.get("start").asInt() + ":" + sourceLocationNode.get("end").asInt();
                }
                String _type = error.get("type").asText();
                ErrorType _errortype = ErrorType.resolve(_type);
                String _component = error.get("component").asText();
                String _severity = error.get("severity").asText();
                if (_severity.equals("error")) {
                    self.compileResult = false;
                }
                String _message = error.get("message").asText();
                String _formattedMessage = error.get("formattedMessage").asText();
                Error parsedError = new Error(_sourceLocation, _errortype, _component, _severity, _message, _formattedMessage);
                self.errors.add(parsedError);
            }

            for (Error error: self.errors) {
                System.out.println(error.toString());
                if(error.type!=ErrorType.WARNING){
                    errorFound = true;
                }
            }
            if(errorFound){
                throw new RuntimeException("compile error in solc");
            }
        }


        if (self.standardOutput.has("contracts")) {
            JsonNode contractsNode = self.standardOutput.get("contracts");

            Iterator<Map.Entry<String, JsonNode>> contractFileIt = contractsNode.fields();
            while (contractFileIt.hasNext()) {
                Map.Entry<String, JsonNode> contractFileNode = contractFileIt.next();
                String fileLocationSlashString = contractFileNode.getKey();

                Sol inputSource = self.solcStandardInput.getSources().get(fileLocationSlashString);
                String fileContent = inputSource.getContent();
                //String fileLocationSlashString = inputSource.fileLocationSlashString;
                //String packageName = inputSource.fullPackageName.toString();
                String packageName = inputSource.getPackage();
                String filePath = inputSource.fileLocation.toString();

                JsonNode ast = self.standardOutput.get("sources").get(fileLocationSlashString).get("ast");
                JsonNode legacyAST = self.standardOutput.get("sources").get(fileLocationSlashString).get("legacyAST");

                JsonNode contractsFilePathValue = contractFileNode.getValue();

                Iterator<Map.Entry<String, JsonNode>> contractNameIt = contractsFilePathValue.fields();
                while (contractNameIt.hasNext()) {
                    Map.Entry<String, JsonNode> contractNameNode = contractNameIt.next();
                    String contractName = contractNameNode.getKey();
                    JsonNode contractNameValue = contractNameNode.getValue();

                    JsonNode abi = contractNameValue.get("abi");

                    JsonNode bytecodeNode = contractNameValue.get("evm").get("bytecode");
                    String bytecode = bytecodeNode.get("object").asText();
                    String sourceMap = bytecodeNode.get("sourceMap").asText();
                    String sourceOpcodes = bytecodeNode.get("opcodes").asText();
                    JsonNode bytecodeLinkReferences = bytecodeNode.get("linkReferences");

                    JsonNode deployedBytecodeNode = contractNameValue.get("evm").get("deployedBytecode");
                    String deployedBytecode = deployedBytecodeNode.get("object").asText();
                    String deployedSourceMap = deployedBytecodeNode.get("sourceMap").asText();
                    String deployedSourceOpcodes = deployedBytecodeNode.get("opcodes").asText();
                    JsonNode deployedBytecodeLinkReferences = deployedBytecodeNode.get("linkReferences");


                    ObjectNode truffle = self.om.createObjectNode();
                    truffle.put("contractName", contractName);
                    truffle.set("abi", abi);
                    truffle.put("bytecode", bytecode);
                    truffle.put("deployedBytecode", deployedBytecode);
                    truffle.put("sourceMap", sourceMap);
                    truffle.put("deployedSourceMap", deployedSourceMap);
                    truffle.put("sourceOpcodes", sourceOpcodes);
                    truffle.put("deployedSourceOpcodes", deployedSourceOpcodes);
                    truffle.set("bytecodeLinkReferences", bytecodeLinkReferences);
                    truffle.set("deployedBytecodeLinkReferences", deployedBytecodeLinkReferences);
                    truffle.put("source", fileContent);
                    truffle.put("sourcePath", fileLocationSlashString);
                    truffle.set("ast", ast);
                    truffle.set("legacyAST", legacyAST);
                    truffle.put("packageName", packageName);
                    truffle.put("sourcePathNative", filePath);
                    //todo
                    SmartContract sourceAndContract = new SmartContract();
                    sourceAndContract.contractName = contractName;
                    sourceAndContract.file = inputSource;
                    self.truffleContracts.put(sourceAndContract, truffle);
                }
            }
        }


        return self;
    }

    public static class Error {
        String sourceLocation;

        public Error(String sourceLocation, ErrorType type, String component, String severity, String message, String formattedMessage) {
            this.sourceLocation = sourceLocation;
            this.type = type;
            this.component = component;
            this.severity = severity;
            this.message = message;
            this.formattedMessage = formattedMessage;
        }


        ErrorType type;

        String component;

        public String toString() {
            return "Error{sourceLocation='" + this.sourceLocation.trim() + "\'" + ", type=" + this.type + ", component='" + this.component + '\'' + ", severity='" + this.severity + '\'' + ", message='" + this.message + '\'' + ", formattedMessage='" + this.formattedMessage + '\'' + '}';
        }


        String severity;

        String message;

        String formattedMessage;
    }

    static enum ErrorType {
        JSONERROR("JSONError", "JSON input doesn’t conform to the required format, e.g. input is not a JSON object, the language is not supported, etc."),
        IOERROR("IOError", "IO and import processing errors, such as unresolvable URL or hash mismatch in supplied sources."),
        PARSERERROR("ParserError", "Source code doesn’t conform to the language rules."),
        DOCSTRINGPARSINGERROR("DocstringParsingError", "The NatSpec tags in the comment block cannot be parsed."),
        SYNTAXERROR("SyntaxError", "Syntactical error, such as continue is used outside of a for loop."),
        DECLARATIONERROR("DeclarationError", "Invalid, unresolvable or clashing identifier names. e.g. Identifier not found."),
        TYPEERROR("TypeError", "Error within the type system, such as invalid type conversions, invalid assignments, etc."),
        UNIMPLEMENTEDFEATUREERROR("UnimplementedFeatureError", "Feature is not supported by the compiler, but is expected to be supported in future versions."),
        INTERNALCOMPILERERROR("InternalCompilerError", "Internal bug triggered in the compiler - this should be reported as an issue."),
        EXCEPTION("Exception", "Unknown failure during compilation - this should be reported as an issue."),
        COMPILERERROR("CompilerError", "Invalid use of the compiler stack - this should be reported as an issue."),
        FATALERROR("FatalError", "Fatal error not processed correctly - this should be reported as an issue."),
        WARNING("Warning", "A warning, which didn’t stop the compilation, but should be addressed if possible.");

        private String error;
        private String description;

        private ErrorType(String error, String description) {
            this.error = error;
            this.description = description;
        }


        public static ErrorType resolve(String error) {
            for (ErrorType e : ErrorType.values()) {
                if (e.error.equals(error)) {
                    return e;
                }
            }
            return null;
        }
    }

    public static class SourceLocation {
        public String file;

        public SourceLocation(String file, long start, long end) {
            this.file = file;
            this.start = start;
            this.end = end;
        }

        long start;
        long end;
    }

}

