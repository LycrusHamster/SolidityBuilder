package me.lycrus;

import java.nio.file.Paths;

public class Utils {

    //Map<String, String> map = new LinkedHashMap<>();

    public String toIndependantPath(String path) {

        String original = Paths.get(path).normalize().toString();
        String ret = original;
        if (ret.matches("[a-zA-Z]:(?:\\\\.+)*")) {
            ret = ret.replaceFirst("([a-zA-Z]):", "/$1");
            ret = ret.replace("\\", "/");
        }
        //map.put(original,ret);
        return ret;
    }

/*    public String getIndependantPathBack(String path){
        if(StringUtils.isEmpty(map.get(path))){
            return path;
        }else {
            return map.get(path);
        }
    }*/

}
