/*
 * Copyright (C) 2021 Vaticle
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.vaticle.typedb.core.server.test;

import com.vaticle.typedb.core.common.collection.Bytes;
import com.vaticle.typedb.core.common.exception.TypeDBException;
import com.vaticle.typedb.core.server.parameters.util.Option;
import com.vaticle.typedb.core.server.parameters.util.YamlParser;
import com.vaticle.typedb.core.server.parameters.Config;
import com.vaticle.typedb.core.server.common.Util;
import com.vaticle.typedb.core.server.parameters.ConfigFactory;
import com.vaticle.typedb.core.server.parameters.ConfigParser;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.HashSet;

import static com.vaticle.typedb.common.collection.Collections.list;
import static com.vaticle.typedb.common.collection.Collections.set;
import static com.vaticle.typedb.core.common.exception.ErrorMessage.Server.CONFIG_FILE_NOT_FOUND;
import static com.vaticle.typedb.core.common.exception.ErrorMessage.Server.CONFIG_OUTPUT_UNRECOGNISED;
import static com.vaticle.typedb.core.common.exception.ErrorMessage.Server.CONFIG_UNEXPECTED_VALUE;
import static com.vaticle.typedb.core.common.exception.ErrorMessage.Server.MISSING_CONFIG_OPTION;
import static com.vaticle.typedb.core.common.exception.ErrorMessage.Server.UNRECOGNISED_CONFIGURATION_OPTIONS;
import static com.vaticle.typedb.core.server.parameters.ConfigFactory.config;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConfigTest {

    @Test
    public void config_file_is_read() {
        Config config = ConfigFactory.config(new ConfigParser());
        assertTrue(config.storage().dataDir().toString().endsWith("server/data"));
        assertEquals(new InetSocketAddress("0.0.0.0", 1729), config.server().address());
        assertEquals(500 * Bytes.MB, config.storage().databaseCache().dataSize());
        assertEquals(500 * Bytes.MB, config.storage().databaseCache().indexSize());
        assertFalse(config.vaticleFactory().enable());
        assertTrue(config.log().output().outputs().containsKey("stdout"));
        assertTrue(config.log().output().outputs().containsKey("file"));
        assertTrue(config.log().output().outputs().get("file").asFile().path().toString().endsWith("server/logs"));
        assertEquals(50 * Bytes.MB, config.log().output().outputs().get("file").asFile().fileSizeCap());
        assertEquals(1 * Bytes.GB, config.log().output().outputs().get("file").asFile().archivesSizeCap());
        assertNotNull(config.log().logger().defaultLogger());
        assertFalse(config.log().logger().defaultLogger().outputs().isEmpty());
        assertEquals("warn", config.log().logger().defaultLogger().level());
        assertFalse(config.log().debugger().reasoner().isEnabled());
    }

    @Test
    public void minimal_config_with_absolute_paths_is_read() {
        Path configMinimalAbsPaths = Util.getTypedbDir().resolve("server/test/config/config-minimal-abs-path.yml");
        Config config = ConfigFactory.config(configMinimalAbsPaths, new HashSet<>(), new ConfigParser());
        assertTrue(config.storage().dataDir().isAbsolute());
        assertEquals(new InetSocketAddress("0.0.0.0", 1730), config.server().address());
        assertEquals(200 * Bytes.MB, config.storage().databaseCache().dataSize());
        assertEquals(700 * Bytes.MB, config.storage().databaseCache().indexSize());
        assertFalse(config.vaticleFactory().enable());
        assertTrue(config.log().output().outputs().containsKey("stdout"));
        assertTrue(config.log().output().outputs().containsKey("file"));
        assertTrue(config.log().output().outputs().get("file").asFile().path().isAbsolute());
        assertEquals(50 * Bytes.MB, config.log().output().outputs().get("file").asFile().fileSizeCap());
        assertEquals(1 * Bytes.GB, config.log().output().outputs().get("file").asFile().archivesSizeCap());
        assertNotNull(config.log().logger().defaultLogger());
        assertFalse(config.log().logger().defaultLogger().outputs().isEmpty());
        assertEquals("warn", config.log().logger().defaultLogger().level());
        assertFalse(config.log().debugger().reasoner().isEnabled());
    }

    @Test
    public void config_invalid_path_throws() {
        Path configMissing = Util.getTypedbDir().resolve("server/test/missing.yml");
        try {
            ConfigFactory.config(configMissing, new HashSet<>(), new ConfigParser());
            fail();
        } catch (TypeDBException e) {
            assert e.code().isPresent();
            assertEquals(CONFIG_FILE_NOT_FOUND.code(), e.code().get());
        }
    }

    @Test
    public void config_file_missing_data_throws() {
        Path configMissingLog = Util.getTypedbDir().resolve("server/test/config/config-missing-data.yml");
        try {
            ConfigFactory.config(configMissingLog, new HashSet<>(), new ConfigParser());
            fail();
        } catch (TypeDBException e) {
            assert e.code().isPresent();
            assertEquals(MISSING_CONFIG_OPTION.code(), e.code().get());
            assertEquals(MISSING_CONFIG_OPTION.message("storage.data"), e.getMessage());
        }
    }

    @Test
    public void config_file_missing_debugger_throws() {
        Path configMissingLogDebugger = Util.getTypedbDir().resolve("server/test/config/config-missing-debugger.yml");
        try {
            ConfigFactory.config(configMissingLogDebugger, new HashSet<>(), new ConfigParser());
            fail();
        } catch (TypeDBException e) {
            assert e.code().isPresent();
            assertEquals(MISSING_CONFIG_OPTION.code(), e.code().get());
            assertEquals(MISSING_CONFIG_OPTION.message("log.debugger"), e.getMessage());
        }
    }

    @Test
    public void config_file_invalid_output_reference_throws() {
        Path configInvalidOutput = Util.getTypedbDir().resolve("server/test/config/config-invalid-logger-output.yml");
        try {
            ConfigFactory.config(configInvalidOutput, new HashSet<>(), new ConfigParser());
            fail();
        } catch (TypeDBException e) {
            assert e.code().isPresent();
            assertEquals(CONFIG_OUTPUT_UNRECOGNISED.code(), e.code().get());
        }
    }

    @Test
    public void config_file_wrong_path_type_throws() {
        Path configInvalidPathType = Util.getTypedbDir().resolve("server/test/config/config-wrong-path-type.yml");
        try {
            ConfigFactory.config(configInvalidPathType, new HashSet<>(), new ConfigParser());
            fail();
        } catch (TypeDBException e) {
            assert e.code().isPresent();
            assertEquals(CONFIG_UNEXPECTED_VALUE.code(), e.code().get());
            assertEquals(CONFIG_UNEXPECTED_VALUE.message("storage.data", "123456[int]", YamlParser.Value.Primitive.PATH.help()), e.getMessage());
        }
    }

    @Test
    public void config_file_unrecognised_option() {
        Path configUnrecognisedOption = Util.getTypedbDir().resolve("server/test/config/config-unrecognised-option.yml");
        try {
            ConfigFactory.config(configUnrecognisedOption, new HashSet<>(), new ConfigParser());
            fail();
        } catch (TypeDBException e) {
            assert e.code().isPresent();
            assertEquals(UNRECOGNISED_CONFIGURATION_OPTIONS.code(), e.code().get());
            assertEquals(UNRECOGNISED_CONFIGURATION_OPTIONS.message(list("log.custom-logger-invalid")), e.getMessage());
        }
    }

    @Test
    public void config_file_accepts_overrides() {
        Config config = ConfigFactory.config(
                set(
                    new Option("storage.data", "server/alt-data"),
                    new Option("server.address", "0.0.0.0:1730"),
                    new Option("log.output.file.directory", "server/alt-logs"),
                    new Option("log.logger.default.level", "info"),
                    new Option("log.logger.typedb.output", "[file]")
                ),
                new ConfigParser()
        );
        assertTrue(config.storage().dataDir().toString().endsWith("server/alt-data"));
        assertEquals(new InetSocketAddress("0.0.0.0", 1730), config.server().address());
        assertFalse(config.vaticleFactory().enable());
        assertTrue(config.log().output().outputs().containsKey("stdout"));
        assertTrue(config.log().output().outputs().containsKey("file"));
        assertTrue(config.log().output().outputs().get("file").asFile().path().toString().endsWith("server/alt-logs"));
        assertEquals(50 * Bytes.MB, config.log().output().outputs().get("file").asFile().fileSizeCap());
        assertEquals(1 * Bytes.GB, config.log().output().outputs().get("file").asFile().archivesSizeCap());
        assertNotNull(config.log().logger().defaultLogger());
        assertFalse(config.log().logger().defaultLogger().outputs().isEmpty());
        assertEquals("info", config.log().logger().defaultLogger().level());
        assertEquals(list("file"), config.log().logger().filteredLoggers().get("typedb").outputs());
        assertFalse(config.log().debugger().reasoner().isEnabled());
    }

    @Test
    public void overrides_list_can_be_yaml_or_repeated() {
        Config config = ConfigFactory.config(
                set(new Option("log.logger.typedb.output", "[file]")),
                new ConfigParser()
        );
        assertEquals(set("file"), set(config.log().logger().filteredLoggers().get("typedb").outputs()));

        Config configWithRepeatedArgs = ConfigFactory.config(
                set(
                    new Option("log.logger.typedb.output", "file"),
                    new Option("log.logger.typedb.output", "stdout")
                ),
                new ConfigParser()
        );
        assertEquals(set("stdout", "file"), set(configWithRepeatedArgs.log().logger().filteredLoggers().get("typedb").outputs()));
    }
}