/*
 * Copyright (C) 2015 Stefan Niederhauser (nidin@gmx.ch)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package guru.nidi.graphviz.engine;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Map;
import java.util.stream.Collectors;

public class GraphvizJdkEngine extends AbstractGraphvizEngine {
    private final static ScriptEngine ENGINE = new ScriptEngineManager().getEngineByExtension("js");

    public GraphvizJdkEngine() {
        this(null);
    }

    public GraphvizJdkEngine(EngineInitListener engineInitListener) {
        super(false, engineInitListener);
        final String[] version = System.getProperty("java.version").split("\\.");
        if (version[1].equals("8") && Integer.parseInt(version[2].substring(2)) > 31) {
            throw new IllegalStateException("JDK 1.8 javascript engines of versions greater than 1.8.0_31 do not run viz.js, sorry!\n" +
                    "Downgrade the JDK, use V8 engine or try with a 1.9 version.");
        }
    }

    @Override
    protected String doExecute(String call) {
        try {
            return (String) ENGINE.eval("$$prints=[]; " + call);
        } catch (ScriptException e) {
            if (e.getMessage().startsWith("abort")) {
                try {
                    throw new GraphvizException(((Map<Integer, Object>) ENGINE.eval("$$prints"))
                            .values()
                            .stream()
                            .map(Object::toString)
                            .collect(Collectors.joining("\n")));
                } catch (ScriptException e1) {
                    //fall through to general exception
                }
            }
            throw new GraphvizException("Problem executing graphviz", e);
        }
    }

    @Override
    protected void doInit() throws Exception {
        ENGINE.eval(initEnv());
        ENGINE.eval(vizCode("1.4.1"));
        ENGINE.eval("Viz('digraph g { a -> b; }');");
    }
}
