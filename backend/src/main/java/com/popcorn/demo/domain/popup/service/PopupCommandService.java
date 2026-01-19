package com.popcorn.demo.domain.popup.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PopupCommandService {
	// TODO(ops-bc): popup bounded context 경계/공통 모듈 정의 (PopupStatus, PopupId, 공통 응답/에러 규격).
	// TODO(ops-event): PopupCreated/Updated/Deleted/StatusChanged 이벤트 클래스 추가.
	// TODO(ops-event): 팝업 등록/수정/삭제 시 ApplicationEventPublisher로 이벤트 발행.
	// TODO(ops-event): 이벤트 리스너에서 노출/검색/알림/통계 처리 분리.
	// TODO: 팝업 등록/수정/삭제 명령 로직 추가 예정
}
