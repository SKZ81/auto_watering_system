idf_component_register(SRCS "main.c"
                            "MCP23017.c"
                            "flowcounter.c"
                    INCLUDE_DIRS "."
                    EMBED_FILES "../files/index.html"
                                "../files/favicon.ico")
                    # PRIV_REQUIRES freertos)
