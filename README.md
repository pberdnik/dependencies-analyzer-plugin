# dependencies-analyzer-plugin

![Build](https://github.com/pberdnik/dependencies-analyzer-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)

<!-- Plugin description -->
## Описание

Плагин помогает с выносом маленького модуля из большого, размечая классы по сложности переноса.

### Пример:
Проект: https://github.com/pberdnik/DependenciesExample

Допустим, `huge` -- большой модуль, из которого хотим перенести классы в новый модуль.
Для этого на вкладке `Green Modules` отмечаем сам модуль `huge` и модули, от которых будет зависеть новый -- допустим, только от `core`

![](res/config_example.png?raw=true)

В дереве проектов выбираем модуль `huge`, в верхнем тулбаре плагина нажимаем кнопку с друмя стрелками (`Run Full Analysis`),
уточняем скоуп в диалоговом окне и запускаем анализ.

p.s. Для больших проектов анализ может быть долгим, поэтому результат сохраняется в .idea.
И если изменений в коде не было, то на вкладке `Green Modules` можно поменять выбор модулей и 
перезапустить анализ второй кнопкой (`Run Graph Analysis`)

![](res/run_example.png?raw=true)

В результате классы размечаются в следующем формате:

`Name.class *размер файла* [*глубина зависимости*] {*наличие цикла*}`

где *глубина зависимоти* = *максимальная глубина из всех зависимостей класса* + 1

Цвет: зеленый -- можно просто перенести, желтый -- переносу мешает одна зависимость, красный -- переносу мешает больше одной зависимости.

Примеры:

`Green2.java 1 [0]` -- у класса нет зависимостей, поэтому глубина 0

`Green1.java 4 [1]` -- у класса есть только зеленые зависимости глубиной 0, так что его глубина на 1 больше

`Cycle1.java 2 [2] {C}` -- желтый, т.к. у класса только одна зависимость, мешающая переносу. {C} -- класс участвует в циклической зависимости.

На панели справа показываются все прямые и обратные зависимости текущего открытого класса.

<!-- Plugin description end -->

![](res/result_example.png?raw=true)

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "dependencies-analyzer-plugin"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/pberdnik/dependencies-analyzer-plugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
