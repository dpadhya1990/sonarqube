/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.license.ws;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.PropertyType;
import org.sonar.api.config.License;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsAction;
import org.sonarqube.ws.Licenses;
import org.sonarqube.ws.Licenses.ListWsResponse;

import static org.sonar.api.CoreProperties.PERMANENT_SERVER_ID;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.license.LicensesWsParameters.ACTION_LIST;

public class ListAction implements WsAction {

  private static final String ALL_SERVERS_VALUE = "*";

  private final UserSession userSession;
  private final PropertyDefinitions definitions;
  private final DbClient dbClient;

  public ListAction(UserSession userSession, PropertyDefinitions definitions, DbClient dbClient) {
    this.userSession = userSession;
    this.definitions = definitions;
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction(ACTION_LIST)
      .setDescription("List licenses settings.<br>" +
        "Requires Administer System' permission")
      .setResponseExample(getClass().getResource("list-example.json"))
      .setSince("6.1")
      .setInternal(true)
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkPermission(SYSTEM_ADMIN);

    DbSession dbSession = dbClient.openSession(true);
    try {
      writeProtobuf(doHandle(dbSession), request, response);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private ListWsResponse doHandle(DbSession dbSession) {
    Set<String> settingsKeys = definitions.getAll().stream()
      .filter(definition -> definition.type().equals(PropertyType.LICENSE))
      .map(PropertyDefinition::key)
      .collect(Collectors.toSet());
    settingsKeys.add(PERMANENT_SERVER_ID);
    List<PropertyDto> properties = dbClient.propertiesDao().selectGlobalPropertiesByKeys(dbSession, settingsKeys);
    return new ListResponseBuilder(settingsKeys, properties).build();
  }

  private static class ListResponseBuilder {
    private final Optional<String> serverId;
    private final Map<String, PropertyDto> licenseSettingsByKey;
    private final Set<String> licenseSettingsKeys;

    ListResponseBuilder(Set<String> licenseSettingsKeys, List<PropertyDto> properties) {
      this.serverId = getServerId(properties);
      this.licenseSettingsKeys = licenseSettingsKeys.stream()
        .filter(key -> !key.equals(PERMANENT_SERVER_ID))
        .collect(Collectors.toSet());
      this.licenseSettingsByKey = properties.stream().collect(Collectors.toMap(PropertyDto::getKey, Function.identity()));
    }

    ListWsResponse build() {
      ListWsResponse.Builder wsResponse = ListWsResponse.newBuilder();
      licenseSettingsKeys.forEach(key -> wsResponse.addLicenses(buildLicense(key, licenseSettingsByKey.get(key))));
      return wsResponse.build();
    }

    private Licenses.License buildLicense(String key, @Nullable PropertyDto setting) {
      Licenses.License.Builder licenseBuilder = Licenses.License.newBuilder().setKey(key);
      if (setting != null) {
        License license = License.readBase64(setting.getValue());
        licenseBuilder.setValue(setting.getValue());
        setProduct(licenseBuilder, license, setting);
        setOrganization(licenseBuilder, license);
        setExpiration(licenseBuilder, license);
        setServerId(licenseBuilder, license);
        setType(licenseBuilder, license);
        setAdditionalProperties(licenseBuilder, license);
      }
      return licenseBuilder.build();
    }

    private static void setProduct(Licenses.License.Builder licenseBuilder, License license, PropertyDto setting) {
      String product = license.getProduct();
      if (product != null) {
        licenseBuilder.setProduct(product);
      }
      if (product == null || !setting.getKey().contains(product)) {
        licenseBuilder.setInvalidProduct(true);
      }
    }

    private static void setOrganization(Licenses.License.Builder licenseBuilder, License license) {
      String licenseOrganization = license.getOrganization();
      if (licenseOrganization != null) {
        licenseBuilder.setOrganization(licenseOrganization);
      }
    }

    private void setServerId(Licenses.License.Builder licenseBuilder, License license) {
      String licenseServerId = license.getServer();
      if (licenseServerId != null) {
        licenseBuilder.setServerId(licenseServerId);
      }
      if (!Objects.equals(ALL_SERVERS_VALUE, licenseServerId) && serverId.isPresent() && !serverId.get().equals(licenseServerId)) {
        licenseBuilder.setInvalidServerId(true);
      }
    }

    private static void setExpiration(Licenses.License.Builder licenseBuilder, License license) {
      String expiration = license.getExpirationDateAsString();
      if (expiration != null) {
        licenseBuilder.setExpiration(expiration);
      }
      if (license.isExpired()) {
        licenseBuilder.setInvalidExpiration(true);
      }
    }

    private static void setType(Licenses.License.Builder licenseBuilder, License license) {
      String type = license.getType();
      if (type != null) {
        licenseBuilder.setType(type);
      }
    }

    private static void setAdditionalProperties(Licenses.License.Builder licenseBuilder, License license) {
      Map<String, String> additionalProperties = license.additionalProperties();
      if (!additionalProperties.isEmpty()) {
        licenseBuilder.getAdditionalPropertiesBuilder().putAllAdditionalProperties(additionalProperties).build();
      }
    }

    private static Optional<String> getServerId(List<PropertyDto> propertyDtos) {
      Optional<PropertyDto> propertyDto = propertyDtos.stream().filter(setting -> setting.getKey().equals(PERMANENT_SERVER_ID)).findFirst();
      return propertyDto.isPresent() ? Optional.of(propertyDto.get().getValue()) : Optional.empty();
    }
  }
}
