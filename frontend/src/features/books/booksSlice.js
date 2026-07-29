import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";

const API_BASE_URL = "";

async function getErrorMessage(response, fallbackMessage) {
  try {
    const errorData = await response.json();
    return errorData.message || fallbackMessage;
  } catch {
    return fallbackMessage;
  }
}

export const fetchBooks = createAsyncThunk(
  "books/fetchBooks",
  async ({ search = "", page = 0, size = 5 } = {}) => {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size),
    });

    if (search.trim()) {
      params.set("search", search.trim());
    }

    const response = await fetch(`${API_BASE_URL}/books?${params.toString()}`);

    if (!response.ok) {
      const message = await getErrorMessage(response, "Failed to fetch books");
      throw new Error(message);
    }

    return await response.json();
  },
);

export const createBook = createAsyncThunk(
  "books/createBook",
  async (bookData) => {
    const response = await fetch(`${API_BASE_URL}/books`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(bookData),
    });

    if (!response.ok) {
      const message = await getErrorMessage(response, "Failed to create book");
      throw new Error(message);
    }

    return await response.json();
  },
);

export const updateBook = createAsyncThunk(
  "books/updateBook",
  async ({ bookId, title, author }) => {
    const response = await fetch(`${API_BASE_URL}/books/${bookId}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        title,
        author,
      }),
    });

    if (!response.ok) {
      const message = await getErrorMessage(response, "Failed to update book");
      throw new Error(message);
    }
    return await response.json();
  },
);

export const deleteBook = createAsyncThunk(
  "books/deleteBook",
  async (bookId) => {
    const response = await fetch(`${API_BASE_URL}/books/${bookId}`, {
      method: "DELETE",
    });

    if (!response.ok) {
      const message = await getErrorMessage(response, "Failed to delete book");
      throw new Error(message);
    }

    return bookId;
  },
);

export const checkBookAvailability = createAsyncThunk(
  "books/checkBookAvailability",
  async (bookId) => {
    const response = await fetch(
      `${API_BASE_URL}/books/${bookId}/availability`,
    );

    if (!response.ok) {
      const message = await getErrorMessage(
        response,
        "Failed to check book availability",
      );
      throw new Error(message);
    }

    const data = await response.json();

    return {
      bookId,
      availability: data,
    };
  },
);

const booksSlice = createSlice({
  name: "books",
  initialState: {
    items: [],
    status: "idle",
    error: null,

    page: 0,
    size: 5,
    totalPages: 0,
    totalElements: 0,

    actionStatus: "idle",
    actionError: null,

    availabilityByBookId: {},
  },
  reducers: {
    clearBookActionError: (state) => {
      state.actionError = null;
      state.actionStatus = "idle";
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchBooks.pending, (state) => {
        state.status = "loading";
        state.error = null;
      })
      .addCase(fetchBooks.fulfilled, (state, action) => {
        state.status = "succeeded";
        state.items = action.payload.content;
        state.page = action.payload.number;
        state.size = action.payload.size;
        state.totalPages = action.payload.totalPages;
        state.totalElements = action.payload.totalElements;
        state.error = null;
      })
      .addCase(fetchBooks.rejected, (state, action) => {
        state.status = "failed";
        state.error = action.error.message;
      })
      .addCase(createBook.pending, (state) => {
        state.actionStatus = "loading";
        state.actionError = null;
      })
      .addCase(createBook.fulfilled, (state) => {
        state.actionStatus = "succeeded";
        state.actionError = null;
      })
      .addCase(createBook.rejected, (state, action) => {
        state.actionStatus = "failed";
        state.actionError = action.error.message;
      })

      .addCase(updateBook.pending, (state) => {
        state.actionStatus = "loading";
        state.actionError = null;
      })
      .addCase(updateBook.fulfilled, (state, action) => {
        state.actionStatus = "succeeded";
        state.actionError = null;

        state.items = state.items.map((book) =>
          book.id === action.payload.id ? action.payload : book,
        );
      })
      .addCase(updateBook.rejected, (state, action) => {
        state.actionStatus = "failed";
        state.actionError = action.error.message;
      })

      .addCase(deleteBook.pending, (state) => {
        state.actionStatus = "loading";
        state.actionError = null;
      })
      .addCase(deleteBook.fulfilled, (state, action) => {
        state.actionStatus = "succeeded";
        state.actionError = null;

        state.items = state.items.filter((book) => book.id !== action.payload);

        delete state.availabilityByBookId[action.payload];
      })
      .addCase(deleteBook.rejected, (state, action) => {
        state.actionStatus = "failed";
        state.actionError = action.error.message;
      })

      .addCase(checkBookAvailability.pending, (state) => {
        state.actionError = null;
      })
      .addCase(checkBookAvailability.fulfilled, (state, action) => {
        const { bookId, availability } = action.payload;
        state.availabilityByBookId[bookId] = availability;
      })
      .addCase(checkBookAvailability.rejected, (state, action) => {
        state.actionError = action.error.message;
      });
  },
});

export const { clearBookActionError } = booksSlice.actions;
export default booksSlice.reducer;
