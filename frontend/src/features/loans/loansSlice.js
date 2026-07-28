import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";

const API_BASE_URL = "";

export function getErrorMessage(response, fallbackMessage) {
  try{
    const errorData = await response.json();
    return errorData.message || fallbackMessage;
  } catch {
    return fallbackMessage;
  }
}

export const fetchLoansByUser = createAsyncThunk(
  "loans/fetchLoansByUser",
  async (userId) => {
    const response = await fetch(`${API_BASE_URL}/users/${userId}/loans`);

    if (!response.ok) {
      const message = await getErrorMessage(
        response, "Failed to fetch loans",
      );
      throw new Error(message);
    }

    return await response.json();
  },
);

export const borrowBook = createAsyncThunk(
  "loans/borrowBook",
  async ({ userId, bookId, days }) => {
    const response = await fetch(`${API_BASE_URL}/users/${userId}/loans`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ bookId, days }),
    });

    if (!response.ok) {
      const message = await getErrorMessage(
        response,
        "Failed to borrow book",
      );
      throw new Error(message);
    }

    return await response.json();
  },
);

export const returnBook = createAsyncThunk(
  "loans/returnBook",
  async ({ userId, bookId }) => {
    const response = await fetch(
      `${API_BASE_URL}/users/${userId}/loans/${bookId}/return`,
      {
        method: "PUT",
      },
    );

    if (!response.ok) {
      throw new Error("Failed to return book");
    }

    return await response.json();
  },
);

export const fetchAdminLoans = createAsyncThunk(
  "loans/fetchAdminLoans",
  async ({
    filter = "active",
    page = 0,
    size = 5,
  } = {}) => {
    let endpoint = "/loans";

    if (filter === "active"){
      endpoint = "/loans/active";
    } else if (filter === "history"){
      endpoint = "/loans/history";
    }

    const params = new URLSearchParams({
      page: String(page),
      size: String(size),
    });

    const response = await fetch(`${API_BASE_URL}${endpoint}?{params.toString()}`,);

    if (!response.ok){
      const message = await getErrorMessage(
        response,
        "Failed to fetch loan records",
      );
      throw new Error(message);
    }

    return await response.json();
  },
);

const loansSlice = createSlice({
  name: "loans",
  initialState: {
    items: [],
    status: "idle",
    error: null,

    adminItems: [],
    adminStatus: "idle",
    adminError: null,
    adminFilter: "active",

    adminPage: 0,
    adminSize: 5,
    adminTotalPages: 0,
    adminTotalElements: 0,
  },
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchLoansByUser.pending, (state) => {
        state.status = "loading";
        state.error = null;
      })
      .addCase(fetchLoansByUser.fulfilled, (state, action) => {
        state.status = "succeeded";
        state.items = action.payload;
        state.error = null;
      })
      .addCase(fetchLoansByUser.rejected, (state, action) => {
        state.status = "failed";
        state.error = action.error.message;
      })
      .addCase(borrowBook.pending, (state) => {
        state.error = null;
      })
      .addCase(borrowBook.fulfilled, (state, action) => {
        state.items.unshift(action.payload);
        state.error = null;
      })
      .addCase(borrowBook.rejected, (state, action) => {
        state.status = "failed";
        state.error = action.error.message;
      })
      .addCase(returnBook.pending, (state) => {
        state.error = null;
      })
      .addCase(returnBook.fulfilled, (state, action) => {
        const returnedLoan = action.payload;

        state.items = state.items.map((loan) =>
          loan.id === returnedLoan.id ? returnedLoan : loan,
        );
      })
      .addCase(returnBook.rejected, (state, action) => {
        state.error = action.error.message;
      })
      .addCase(fetchAdminLoans.pending, (state, action) => {
        state.adminStatus = "loading";
        state.adminError = null;
        state.adminFilter = action.meta.arg?.filter || "active";
      })
      .addCase(fetchAdminLoans.fulfilled, (state, action) => {
        state.adminStatus = "succeeded";

        state.adminItems = action.payload.content;
        state.adminPage = action.payload.number;
        state.adminSize = action.payload.size;
        state.adminTotalPages = action.payload.totalPages;
        state.adminTotalElements = action.payload.totalElements;
        
        state.adminError = null;
      })
      .addCase(fetchAdminLoans.rejected, (state, action) => {
        state.adminStatus = "failed";
        state.adminError = action.error.message;
      });
  },
});

export default loansSlice.reducer;
