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
export const START_LOADING = 'settingsPage/START_LOADING';

export const startLoading = key => ({
  type: START_LOADING,
  key
});

export const FAIL_VALIDATION = 'settingsPage/FAIL_VALIDATION';

export const failValidation = (key, message) => ({
  type: FAIL_VALIDATION,
  key,
  message
});

export const PASS_VALIDATION = 'settingsPage/PASS_VALIDATION';

export const passValidation = key => ({
  type: PASS_VALIDATION,
  key
});