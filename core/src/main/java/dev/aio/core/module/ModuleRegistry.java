package dev.aio.core.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModuleRegistry {
    private final Map<String, Module> byId = new LinkedHashMap<>();
    private final Map<Class<?>, Module> byClass = new HashMap<>();

    public <M extends Module> M register(M module) {
        if (byId.containsKey(module.id())) throw new IllegalArgumentException("Modul-ID bereits vergeben: " + module.id());
        byId.put(module.id(), module);
        byClass.put(module.getClass(), module);
        return module;
    }

    @SuppressWarnings("unchecked")
    public <M extends Module> M get(Class<M> type) {
        return (M) byClass.get(type);
    }

    public Module get(String id) { return byId.get(id); }

    public List<Module> all() { return Collections.unmodifiableList(new ArrayList<>(byId.values())); }

    public List<Module> byCategory(Category category) {
        List<Module> result = new ArrayList<>();
        for (Module m : byId.values()) if (m.category() == category) result.add(m);
        return result;
    }

    public <T> List<T> ofType(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Module m : byId.values()) if (type.isInstance(m)) result.add(type.cast(m));
        return result;
    }
}
