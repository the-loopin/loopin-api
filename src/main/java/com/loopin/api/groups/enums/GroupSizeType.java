package com.loopin.api.groups.enums;

public enum GroupSizeType {
    TWO(2),
    THREE(3),
    FOUR(4),
    FOUR_PLUS(10);

    private final int maxMembers;

    GroupSizeType(int maxMembers) {
        this.maxMembers = maxMembers;
    }

    public int getMaxMembers() {
        return maxMembers;
    }
}
