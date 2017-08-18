package com.o2osys.entity.daum;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ResKeyword {
	@JsonProperty("channel")
	private Channel channel;

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Channel {
		@JsonProperty("info")
		private Info info;

		@JsonProperty("item")
		private ArrayList<Item> items;

		@Data
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public static class Item {
			@JsonProperty("related_place_count")
			private long relatedPlaceCount;

			@JsonProperty("related_place")
			private String relatedPlace;

			@JsonProperty("categoryCode")
			private String categoryCode;
			
			@JsonProperty("phone")
			private String phone;

			@JsonProperty("newAddress")
			private String newAddress;

			@JsonProperty("imageUrl")
			private String imageUrl;

			@JsonProperty("direction")
			private String direction;

			@JsonProperty("zipcode")
			private String zipcode;

			@JsonProperty("placeUrl")
			private String placeUrl;

			@JsonProperty("id")
			private String id;

			@JsonProperty("title")
			private String title;

			@JsonProperty("distance")
			private String distance;

			@JsonProperty("category")
			private String category;

			@JsonProperty("address")
			private String address;

			@JsonProperty("longitude")
			private String longitude;

			@JsonProperty("latitude")
			private String latitude;

			@JsonProperty("addressBCode")
			private String addressBCode;
		}

		@Data
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public static class Info {
			@JsonProperty("count")
			private String count;

			@JsonProperty("page")
			private String page;

			@JsonProperty("totalCount")
			private String totalCount;

			@JsonProperty("samename")
			private Samename samename;

			@Data
			@JsonInclude(JsonInclude.Include.NON_NULL)
			public static class Samename {
				@JsonProperty("region")
				private String[] region;
				
				@JsonProperty("keyword")
				private String keyword;

				@JsonProperty("selected_region")
				private String selectedRegion;
			}
		}
	}
}
