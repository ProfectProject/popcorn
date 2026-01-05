package com.popcorn.demo.domain.popup.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PopupEventHandler {

	private static final Logger log = LoggerFactory.getLogger(PopupEventHandler.class);

	@EventListener
	public void handleSearch(PopupSearchEvent event) {
		log.info("🔎 팝업 검색 이벤트 - category: {}, regionId: {}, keyword: {}, total: {}",
				event.getCategory(), event.getRegionId(), event.getKeyword(), event.getTotal());
	}

	@EventListener
	public void handleViewed(PopupViewedEvent event) {
		log.info("👀 팝업 조회 이벤트 - popupId: {}, storeId: {}, category: {}",
				event.getPopupId(), event.getStoreId(), event.getCategory());
	}
}
