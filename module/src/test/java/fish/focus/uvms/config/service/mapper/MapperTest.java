/*
﻿Developed with the contribution of the European Commission - Directorate General for Maritime Affairs and Fisheries
© European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can
redistribute it and/or modify it under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or any later version. The IFDM Suite is distributed in
the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. You should have received a
copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */
package fish.focus.uvms.config.service.mapper;

import fish.focus.schema.config.types.v1.SettingType;
import fish.focus.uvms.config.service.MockData;
import fish.focus.uvms.config.service.entity.component.Setting;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

@RunWith(MockitoJUnitRunner.class)
public class MapperTest {


    @InjectMocks
    private ConfigMapper mapper;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testEntityToModel() {
        Long id = 1L;
        Setting entity = MockData.getEntity(id);

        SettingType result = mapper.toModel(entity);

        assertEquals(id, result.getId());
        assertSame(entity.getValue(), result.getValue());
    }

    @Test
    public void testModelToEntity() {
        Long id = 1L;
        String user = "testUser";
        SettingType model = MockData.getModel(id);

        Setting result = mapper.toEntity(model, user);

        assertSame(model.getValue(), result.getValue());
    }

    @Test
    public void testEntityAndModelToEntity() {
        Long id = 1L;
        String user = "testUser";
        Setting entity = MockData.getEntity(id);
        SettingType vessel = MockData.getModel(1L);

        Setting result = mapper.toEntity(entity, vessel, user);

        assertSame(result.getValue(), vessel.getValue());
    }

    @Test
    public void testEntityAndModelToModel() {
        Setting entity = MockData.getEntity(1L);
        SettingType result = mapper.mapToModel(null, entity);

        assertSame(result.getValue(), entity.getValue());
    }
}