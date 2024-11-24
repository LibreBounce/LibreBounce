package net.ccbluex.liquidbounce.utils.mappings

import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.io.resource
import net.fabricmc.mappings.model.V2MappingsProvider

object EnvironmentRemapper {

    private var mappings = runCatching {
        V2MappingsProvider.readTinyMappings(resource("/mappings/mappings.tiny").bufferedReader())
    }.onFailure {
        logger.error("Unable to load mappings. Ignore this if you are using a development environment.", it)
    }.getOrNull()

    private var environment = runCatching {
        probeEnvironment()
    }.onFailure {
        logger.error("Unable to probe environment. Please make sure you are using a valid environment.", it)
    }.getOrNull()

    private fun probeEnvironment(): String? {
        val mappings = mappings ?: return null

        val minecraftClassEntry = mappings.classEntries?.find { entry ->
            entry?.get("named") == "net/minecraft/client/MinecraftClient"
        }

        if (minecraftClassEntry == null) {
            logger.error("Unable to probe environment. Please make sure you are using a valid environment.")
            return null
        }

        logger.info("Probing environment...")
        return when {
            isClassPresent(minecraftClassEntry.get("intermediary")?.toDotNotation()) -> {
                logger.info("Intermediary environment detected.")
                "intermediary"
            }
            else -> {
                logger.error("No matching environment detected. Please make sure you are using a valid environment.")
                null
            }
        }
    }

    private fun isClassPresent(className: String?): Boolean {
        return try {
            Class.forName(className)
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    fun remapClassName(clazz: String): String {
        environment ?: return clazz

        val className = clazz.toSlashNotation()
        return mappings?.classEntries?.find {
            it?.get("named") == className
        }?.get(environment)?.toDotNotation() ?: clazz
    }

    fun remapClassNameToNamed(clazz: String): String {
        environment ?: return clazz

        val className = clazz.toSlashNotation()
        return mappings?.classEntries?.find {
            it?.get(environment) == className
        }?.get("named")?.toDotNotation() ?: clazz
    }

    fun remapField(clazz: Class<*>, name: String): String {
        environment ?: return name

        val clazzNames = getClassHierarchyNames(clazz)

        return mappings?.fieldEntries?.find { entry ->
            val intern = entry.get(environment)
            clazzNames.contains(intern.owner) && intern.name == name
        }?.get("named")?.name ?: name
    }

    fun remapMethod(clazz: Class<*>, name: String): String {
        environment ?: return name

        val clazzNames = getClassHierarchyNames(clazz)

        return mappings?.methodEntries?.find { entry ->
            val intern = entry.get(environment)
            clazzNames.contains(intern.owner) && intern.name == name
        }?.get("named")?.name ?: name
    }

    private fun getClassHierarchyNames(clazz: Class<*>): Set<String> {
        val clazzNames = mutableSetOf(clazz.name.toSlashNotation())
        var current = clazz

        while (current.name != "java.lang.Object") {
            current = current.superclass ?: break
            clazzNames.add(current.name.toSlashNotation())
        }

        return clazzNames
    }

    private fun String.toDotNotation(): String = replace('/', '.')

    private fun String.toSlashNotation(): String = replace('.', '/')

}
