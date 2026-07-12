package com.loopin.api.groups.api;

import com.loopin.api.users.entity.User;

import java.util.List;

/**
 * Users eligible for badges after one group has been archived.
 */
public record ArchivedGroupAwardRecipients(User creator, List<User> members) {
}
