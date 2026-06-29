package office_mate_2605.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BCryptUtilTest {

    @Test
    void hashPasswordCreatesBCryptHash() {
        String hashed = BCryptUtil.hashPassword("test123");

        assertNotNull(hashed);
        assertTrue(hashed.startsWith("$2"));
        assertEquals(60, hashed.length());
        assertEquals(12, BCryptUtil.getWorkFactor(hashed));
    }

    @Test
    void checkPasswordReturnsTrueOnlyForMatchingPassword() {
        String hashed = BCryptUtil.hashPassword("test123");

        assertTrue(BCryptUtil.checkPassword("test123", hashed));
        assertFalse(BCryptUtil.checkPassword("wrongpass", hashed));
    }

    @Test
    void samePasswordCreatesDifferentHashesBecauseOfSalt() {
        String hash1 = BCryptUtil.hashPassword("test123");
        String hash2 = BCryptUtil.hashPassword("test123");
        String hash3 = BCryptUtil.hashPassword("test123");

        assertNotEquals(hash1, hash2);
        assertNotEquals(hash2, hash3);
        assertNotEquals(hash1, hash3);
        assertTrue(BCryptUtil.checkPassword("test123", hash1));
        assertTrue(BCryptUtil.checkPassword("test123", hash2));
        assertTrue(BCryptUtil.checkPassword("test123", hash3));
    }

    @Test
    void hashPasswordRejectsBlankPassword() {
        assertThrows(IllegalArgumentException.class, () -> BCryptUtil.hashPassword(null));
        assertThrows(IllegalArgumentException.class, () -> BCryptUtil.hashPassword(""));
        assertThrows(IllegalArgumentException.class, () -> BCryptUtil.hashPassword("   "));
    }

    @Test
    void checkPasswordReturnsFalseForInvalidInput() {
        String hashed = BCryptUtil.hashPassword("test123");

        assertFalse(BCryptUtil.checkPassword(null, hashed));
        assertFalse(BCryptUtil.checkPassword("", hashed));
        assertFalse(BCryptUtil.checkPassword("test123", null));
        assertFalse(BCryptUtil.checkPassword("test123", ""));
        assertFalse(BCryptUtil.checkPassword("test123", "not-a-bcrypt-hash"));
    }
}
