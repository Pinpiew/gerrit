// Copyright (C) 2017 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gerrit.server.query.group;

import com.google.gerrit.reviewdb.client.AccountGroup;
import com.google.gerrit.server.index.Schema;
import com.google.gerrit.server.index.group.GroupSchemaDefinitions;
import com.google.gerrit.testutil.ConfigSuite;
import com.google.gerrit.testutil.InMemoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.eclipse.jgit.lib.Config;

public class LuceneQueryGroupsTest extends AbstractQueryGroupsTest {
  @ConfigSuite.Config
  public static Config againstPreviousIndexVersion() {
    Config cfg = defaultConfig();
    Schema<AccountGroup> prevSchema = GroupSchemaDefinitions.INSTANCE.getPrevious();
    if (prevSchema != null) {
      cfg.setInt(
          "index",
          "lucene",
          GroupSchemaDefinitions.INSTANCE.getName() + "TestVersion",
          prevSchema.getVersion());
    }
    return cfg;
  }

  @Override
  protected Injector createInjector() {
    Config luceneConfig = new Config(config);
    InMemoryModule.setDefaults(luceneConfig);
    return Guice.createInjector(new InMemoryModule(luceneConfig, notesMigration));
  }
}
