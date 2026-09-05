#!/usr/bin/env python3
from pathlib import Path
p = Path("app/src/main/java/cn/lineai/mvp/MainControllerInitializer.java")
s = p.read_text()
old = '''                                "data:import_linecode"
                        )
                    }'''
new = '''                                "data:import_linecode"
                        );
                    }'''
if old not in s:
    if ");\n                    }" in s and "data:import_linecode" in s:
        print("already fixed")
    else:
        raise SystemExit("pattern missing")
else:
    p.write_text(s.replace(old, new, 1))
    print("semicolon restored")
