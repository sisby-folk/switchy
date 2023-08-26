/*
 * Copyright 2022, 2023 QuiltMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package folk.sisby.switchy;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.InMemoryCommentedFormat;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import com.electronwill.nightconfig.core.io.ConfigParser;
import com.electronwill.nightconfig.core.io.ConfigWriter;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import net.fabricmc.loader.FabricLoader;
import org.quiltmc.config.api.*;
import org.quiltmc.config.api.annotations.Comment;
import org.quiltmc.config.api.values.*;
import org.quiltmc.config.impl.ConfigImpl;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("deprecation")
public class QuiltifiedFabricConfig {
	public static <T extends WrappedConfig> T create(String familyId, String id, Class<T> configCreatorClass) {
		return ConfigImpl.create(ENV, familyId, id, Paths.get(""), b -> {}, configCreatorClass, b -> {});
	}

	public static ConfigEnvironment ENV = new ConfigEnvironment(FabricLoader.INSTANCE.getConfigDir(), "toml", new NightConfigSerializer<>("toml", new TomlParser(), new TomlWriter()));

	public static final class NightConfigSerializer<C extends CommentedConfig> implements Serializer {
		private final String fileExtension;
		private final ConfigParser<C> parser;
		private final ConfigWriter writer;

		public NightConfigSerializer(String fileExtension, ConfigParser<C> parser, ConfigWriter writer) {
			this.fileExtension = fileExtension;
			this.parser = parser;
			this.writer = writer;
		}

		@Override
		public String getFileExtension() {
			return this.fileExtension;
		}

		@Override
		public void serialize(Config config, OutputStream to) {
			this.writer.write(write(createCommentedConfig(), config.nodes()), to);
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		@Override
		public void deserialize(Config config, InputStream from) {
			CommentedConfig read = this.parser.parse(from);

			for (TrackedValue<?> trackedValue : config.values()) {
				if (read.contains(trackedValue.key().toString())) {
					((TrackedValue) trackedValue).setValue(MarshallingUtils.coerce(read.get(trackedValue.key().toString()), trackedValue.getDefaultValue(), (CommentedConfig c, MarshallingUtils.MapEntryConsumer entryConsumer) ->
						c.entrySet().forEach(e -> entryConsumer.put(e.getKey(), e.getValue()))), false);
				}
			}
		}

		private static List<Object> convertList(List<?> list) {
			List<Object> result = new ArrayList<>(list.size());

			for (Object value : list) {
				result.add(convertAny(value));
			}

			return result;
		}

		private static UnmodifiableCommentedConfig convertMap(ValueMap<?> map) {
			CommentedConfig result = createCommentedConfig();

			for (Map.Entry<String, ?> entry : map.entrySet()) {
				result.add(entry.getKey(), convertAny(entry.getValue()));
			}

			return result;
		}

		private static Object convertAny(Object value) {
			if (value instanceof ValueMap) {
				return convertMap((ValueMap<?>) value);
			} else if (value instanceof ValueList) {
				return convertList((ValueList<?>) value);
			} else if (value instanceof ConfigSerializableObject) {
				return convertAny(((ConfigSerializableObject<?>) value).getRepresentation());
			} else {
				return value;
			}
		}

		private static CommentedConfig write(CommentedConfig config, Iterable<ValueTreeNode> nodes) {
			for (ValueTreeNode node : nodes) {
				List<String> comments = new ArrayList<>();

				if (node.hasMetadata(Comment.TYPE)) {
					for (String string : node.metadata(Comment.TYPE)) {
						comments.add(string);
					}
				}

				if (node instanceof TrackedValue<?> trackedValue) {
					Object defaultValue = trackedValue.getDefaultValue();

					if (defaultValue.getClass().isEnum()) {
						StringBuilder options = new StringBuilder("options: ");
						Object[] enumConstants = defaultValue.getClass().getEnumConstants();

						for (int i = 0, enumConstantsLength = enumConstants.length; i < enumConstantsLength; i++) {
							Object o = enumConstants[i];

							options.append(o);

							if (i < enumConstantsLength - 1) {
								options.append(", ");
							}
						}

						comments.add(options.toString());
					}

					for (Constraint<?> constraint : trackedValue.constraints()) {
						comments.add(constraint.getRepresentation());
					}

					if (!(defaultValue instanceof CompoundConfigValue<?>)) {
						comments.add("default: " + defaultValue);
					}

					config.add(trackedValue.key().toString(), convertAny(trackedValue.getRealValue()));
				} else {
					write(config, ((ValueTreeNode.Section) node));
				}

				if (!comments.isEmpty()) {
					config.setComment(node.key().toString(), " " + String.join("\n ", comments));
				}
			}

			return config;
		}

		private static CommentedConfig createCommentedConfig() {
			return InMemoryCommentedFormat.defaultInstance().createConfig(LinkedHashMap::new);
		}
	}
}
