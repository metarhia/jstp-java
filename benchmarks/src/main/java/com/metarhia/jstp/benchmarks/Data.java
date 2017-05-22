package com.metarhia.jstp.benchmarks;

public final class Data {

  public static final String EMPTY_OBJECT = "{}";

  public static final String JSTP_COMPLEX_OBJECT = "{\n" +
      "    login: {\n" +
      "      control: 'screen',\n" +
      "      array: [12, '111', 'screen'],\n" +
      "      controls: {\n" +
      "        login: {\n" +
      "          control: 'edit',\n" +
      "          filter: 'login',\n" +
      "          top: 10, left: 10, right: 10,\n" +
      "          height: 10,\n" +
      "          label: 'login'\n" +
      "        },\n" +
      "        password: {\n" +
      "          control: 'edit',\n" +
      "          mode: 'password',\n" +
      "          top: 25, left: 10, right: 10,\n" +
      "          height: 10,\n" +
      "          label: 'password'\n" +
      "        },\n" +
      "        cancel: {\n" +
      "          control: 'button',\n" +
      "          top: 40, right: 70,\n" +
      "          width: 25, height: 10,\n" +
      "          text: 'Cancel'\n" +
      "        },\n" +
      "        signin: {\n" +
      "          control: 'button',\n" +
      "          top: 40, right: 10,\n" +
      "          width: 25, height: 10,\n" +
      "          text: 'Sign in'\n" +
      "        },\n" +
      "        social: {\n" +
      "          control: 'panel',\n" +
      "          top: 55, botton: 10, left: 10, right: 10,\n" +
      "          controls: [\n" +
      "            {\n" +
      "              control: 'button',\n" +
      "              top: 0, left: 0,\n" +
      "              height: 10, width: 10,\n" +
      "              image: 'googlePlus'\n" +
      "            },\n" +
      "            {\n" +
      "              control: 'button',\n" +
      "              top: 0, left: 10,\n" +
      "              height: 10, width: 10,\n" +
      "              image: 'facebook'\n" +
      "            },\n" +
      "            {\n" +
      "              control: 'button',\n" +
      "              top: 0, left: 10,\n" +
      "              height: 10, width: 10,\n" +
      "              image: 'vk'\n" +
      "            },\n" +
      "            {\n" +
      "              control: 'button',\n" +
      "              top: 0, left: 20,\n" +
      "              height: 10, width: 10,\n" +
      "              image: 'twitter'\n" +
      "            }\n" +
      "          ]\n" +
      "        }\n" +
      "      }\n" +
      "    },\n" +
      "    main: {\n" +
      "      control: 'screen',\n" +
      "      controls: {\n" +
      "        message: {\n" +
      "          control: 'label',\n" +
      "          top: 10, left: 10, right: 10,\n" +
      "          height: 10,\n" +
      "          text: 'You are logged in'\n" +
      "        }\n" +
      "    }\n" +
      "   }\n" +
      "}";

  public static final String JSTP_CONSOLE_LAYOUT = "{\n" +
      "    login: {\n" +
      "      control: 'screen',\n" +
      "      controls: {\n" +
      "        login: {\n" +
      "          control: 'edit',\n" +
      "          filter: 'login',\n" +
      "          top: 10, left: 10, right: 10,\n" +
      "          height: 10,\n" +
      "          label: 'login'\n" +
      "        },\n" +
      "        password: {\n" +
      "          control: 'edit',\n" +
      "          mode: 'password',\n" +
      "          top: 25, left: 10, right: 10,\n" +
      "          height: 10,\n" +
      "          label: 'password'\n" +
      "        },\n" +
      "        cancel: {\n" +
      "          control: 'button',\n" +
      "          top: 40, right: 70,\n" +
      "          width: 25, height: 10,\n" +
      "          text: 'Cancel'\n" +
      "        },\n" +
      "        signin: {\n" +
      "          control: 'button',\n" +
      "          top: 40, right: 10,\n" +
      "          width: 25, height: 10,\n" +
      "          text: 'Sign in'\n" +
      "        },\n" +
      "        social: {\n" +
      "          control: 'panel',\n" +
      "          top: 55, botton: 10, left: 10, right: 10,\n" +
      "          controls: {\n" +
      "            {\n" +
      "              control: 'button',\n" +
      "              top: 0, left: 0,\n" +
      "              height: 10, width: 10,\n" +
      "              image: 'googlePlus'\n" +
      "            },\n" +
      "            {\n" +
      "              control: 'button',\n" +
      "              top: 0, left: 10,\n" +
      "              height: 10, width: 10,\n" +
      "              image: 'facebook'\n" +
      "            },\n" +
      "            {\n" +
      "              control: 'button',\n" +
      "              top: 0, left: 10,\n" +
      "              height: 10, width: 10,\n" +
      "              image: 'vk'\n" +
      "            },\n" +
      "            {\n" +
      "              control: 'button',\n" +
      "              top: 0, left: 20,\n" +
      "              height: 10, width: 10,\n" +
      "              image: 'twitter'\n" +
      "            }\n" +
      "          }\n" +
      "        }\n" +
      "      }\n" +
      "    },\n" +
      "    main: {\n" +
      "      control: 'screen',\n" +
      "      controls: {\n" +
      "        message: {\n" +
      "          control: 'label',\n" +
      "          top: 10, left: 10, right: 10,\n" +
      "          height: 10,\n" +
      "          text: 'You are logged in'\n" +
      "        }\n" +
      "    }\n" +
      "   }\n" +
      "}";
}
