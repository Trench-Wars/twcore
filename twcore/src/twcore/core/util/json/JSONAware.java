package twcore.core.util.json;

/**
    Beans that support customized output of JSON text shall implement this interface.
    @author <a href="mailto:fangyidong@yahoo.com.cn">FangYidong</a>
*/
public interface JSONAware {
    /**
        @return JSON text
    */
    String toJSONString();
}
