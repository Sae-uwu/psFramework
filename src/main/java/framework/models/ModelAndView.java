package framework.models;

import java.util.HashMap;
import java.util.Map;

public class ModelAndView {

    private String view;
    private Map<String, Object> data;

    public ModelAndView(String view) {
        this.view = view;
        this.data = new HashMap<>();
    }

    public void addAttribute(String name, Object value) {
        data.put(name, value);
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
