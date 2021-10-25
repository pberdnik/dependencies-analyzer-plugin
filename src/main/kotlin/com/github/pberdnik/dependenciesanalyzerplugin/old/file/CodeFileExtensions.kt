package com.github.pberdnik.dependenciesanalyzerplugin.old.file

val CodeFile.module get() = path.substring(path.indexOf("$/") + 2).substringBefore('/')
val CodeFile.shortPath get() = if (module == "lib-bro") {
    path.substringAfter("com/yandex/browser/").substringAfter('/')
} else {
    path.substringAfter(module)
}
val CodeFile.libBroPackage get() = if (module == "lib-bro") {
    path.substringAfter("com/yandex/browser/").substringBefore('/')
} else {
    ""
}
val CodeFile.className get() = path.substringAfterLast('/')