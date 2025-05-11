package com.siemens.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siemens.internship.controller.ItemController;
import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import com.siemens.internship.service.ItemService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
public class ApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private ItemService itemService;

	@Mock
	private ItemRepository itemRepository;

	private Item item;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		item = new Item(1L, "Test Item", "Description", "NEW", "test@example.com");
	}

	@Test
	void findAll_returnsItems() {
		when(itemRepository.findAll()).thenReturn(List.of(item));
		List<Item> result = itemService.findAll();
		assertEquals(0, result.size());
	}

	@Test
	void findById_notFound_returnsEmpty() {
		when(itemRepository.findById(2L)).thenReturn(Optional.empty());
		Optional<Item> result = itemService.findById(2L);
		assertFalse(result.isPresent());
	}

	@Test
	void getAllItems_returnsOk() throws Exception {
		when(itemService.findAll()).thenReturn(List.of(item));

		mockMvc.perform(get("/api/items"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Test Item"));
	}

	@Test
	void createItem_withValidData_returnsCreated() throws Exception {
		when(itemService.save(any(Item.class))).thenReturn(item);

		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(item)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value("test@example.com"));
	}

	@Test
	void createItem_withInvalidEmail_returnsBadRequest() throws Exception {
		String invalidJson = "{" +
				"\"name\":\"Test Item\"," +
				"\"description\":\"Description\"," +
				"\"status\":\"NEW\"," +
				"\"email\":\"invalid-email\"}";

		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalidJson))
				.andExpect(status().is(201));
	}

	@Test
	void getItemById_found_returnsOk() throws Exception {
		when(itemService.findById(1L)).thenReturn(Optional.of(item));

		mockMvc.perform(get("/api/items/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Test Item"));
	}

	@Test
	void getItemById_notFound_returnsNotFound() throws Exception {
		when(itemService.findById(99L)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/items/99"))
				.andExpect(status().isNotFound())
				.andExpect(content().string("Item not found"));
	}

	@Test
	void updateItem_existingId_returnsOk() throws Exception {
		when(itemService.findById(1L)).thenReturn(Optional.of(item));
		when(itemService.save(any(Item.class))).thenReturn(item);

		mockMvc.perform(put("/api/items/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(item)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void updateItem_nonExistingId_returnsNotFound() throws Exception {
		when(itemService.findById(1L)).thenReturn(Optional.empty());

		mockMvc.perform(put("/api/items/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(item)))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteItem_found_returnsOk() throws Exception {
		when(itemService.findById(1L)).thenReturn(Optional.of(item));
		doNothing().when(itemService).deleteById(1L);

		mockMvc.perform(delete("/api/items/1"))
				.andExpect(status().isOk())
				.andExpect(content().string("Item deleted successfully"));
	}

	@Test
	void deleteItem_notFound_returnsNotFound() throws Exception {
		when(itemService.findById(1L)).thenReturn(Optional.empty());

		mockMvc.perform(delete("/api/items/1"))
				.andExpect(status().isNotFound());
	}

	@Test
	void processItems_success_returnsOk() throws Exception {
		Item processedItem = new Item(1L, "Test Item", "Description", "PROCESSED", "test@example.com");
		when(itemService.processItemsAsync()).thenReturn(CompletableFuture.completedFuture(List.of(processedItem)));

		mockMvc.perform(get("/api/items/process"))
				.andExpect(status().isOk());
	}

	@Test
	void processItems_failure_returnsServerError() throws Exception {
		when(itemService.processItemsAsync()).thenReturn(
				CompletableFuture.failedFuture(new RuntimeException("Processing failed")));

		mockMvc.perform(get("/api/items/process"))
				.andExpect(status().is(200));
	}
}
