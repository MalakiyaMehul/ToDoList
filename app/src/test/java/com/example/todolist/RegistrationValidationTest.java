package com.example.todolist;

import com.example.todolist.activity.Supabase.SupabaseRepository;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RegistrationValidationTest {

    @Test
    public void passwordStrength_handlesEmptyAndLow() {
        assertEquals(InputField.STRENGTH_NONE, InputField.calculatePasswordStrength(null));
        assertEquals(InputField.STRENGTH_NONE, InputField.calculatePasswordStrength(""));
        assertEquals(InputField.STRENGTH_LOW, InputField.calculatePasswordStrength("123"));
        assertEquals(InputField.STRENGTH_LOW, InputField.calculatePasswordStrength("abcdef"));
        assertEquals(InputField.STRENGTH_LOW, InputField.calculatePasswordStrength("123456"));
    }

    @Test
    public void passwordStrength_handlesMediumHighAndStrong() {
        assertEquals(InputField.STRENGTH_MEDIUM, InputField.calculatePasswordStrength("pass12"));
        assertEquals(InputField.STRENGTH_MEDIUM, InputField.calculatePasswordStrength("abcdefgh"));
        assertEquals(InputField.STRENGTH_HIGH, InputField.calculatePasswordStrength("Pass1234"));
        assertEquals(InputField.STRENGTH_HIGH, InputField.calculatePasswordStrength("password12"));
        assertEquals(InputField.STRENGTH_STRONG, InputField.calculatePasswordStrength("Pass@123"));
        assertEquals(InputField.STRENGTH_STRONG, InputField.calculatePasswordStrength("Pass123456"));
    }

    @Test
    public void escapeIlikeLiteral_escapesSpecialWildcards() {
        assertEquals("", SupabaseRepository.escapeIlikeLiteral(null));
        assertEquals("test\\_user\\_901", SupabaseRepository.escapeIlikeLiteral("test_user_901"));
        assertEquals("100\\%\\\\test", SupabaseRepository.escapeIlikeLiteral("100%\\test"));
    }
}
