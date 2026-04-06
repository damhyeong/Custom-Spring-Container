package com.damsoon.util;

public enum DependencyMode {
    NORMAL, // 일반 의존성 해결의 경우에 적용
    LAZY, // 의존성 해결이 "후 처리" 로직일 경우 적용,
    REST // 표식도 존재하지 않은 완전한 "순환 참조" 일 경우 적용
}
