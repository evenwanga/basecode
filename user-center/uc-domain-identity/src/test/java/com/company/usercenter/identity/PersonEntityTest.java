package com.company.usercenter.identity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Person 实体测试")
class PersonEntityTest {

    @Test
    @DisplayName("新建 Person 应默认状态为 ACTIVE")
    void newPerson_shouldHaveActiveStatus() {
        Person person = new Person();
        assertThat(person.getStatus()).isEqualTo("ACTIVE");
    }
}
