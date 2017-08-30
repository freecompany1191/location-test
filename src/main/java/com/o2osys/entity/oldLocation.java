package com.o2osys.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class oldLocation {
	@JsonProperty("latitude")
	private String latitude;

	@JsonProperty("longitude")
	private String longitude;

	@JsonProperty("xy_acc_type")
	private String xyAccType;

	@JsonProperty("old_addr")
	private String oldAddr;

	@JsonProperty("new_addr")
	private String newAddr;

	@JsonProperty("local_name_1")
	private String localName1;

	@JsonProperty("local_name_2")
	private String localName2;

	@JsonProperty("local_name_3")
	private String localName3;
}
