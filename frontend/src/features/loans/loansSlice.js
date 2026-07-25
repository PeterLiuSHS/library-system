import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";

const API_BASE_URL = "";

export const fetchLoansByUser = createAsyncThunk(
  "loans/fetchLoansByUser",
  async (userId) => {
    const response = await fetch(`${API_BASE_URL}/users/${userId}/loans`);

    if (!response.ok) {
      throw new Error("Failed to fetch loans");
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
      const errorData = await response.json();
      throw new Error(errorData.message || "Failed to borrow book");
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
  async (filter) => {
    let endpoint = "/loans";

    if (filter === "active"){
      endpoint = "/loans/active";
    } else if (filter === "history"){
      endpoint = "/loans/history";
    }

    const response = await fetch(`${API_BASE_URL}${endpoint}`);

    if (!response.ok){
      const errorData = await response.json();
      throw new Error(
        errorData.message || "Failed to fetch loan records",
      );
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
  },
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchLoansByUser.pending, (state) => {
        state.status = "loading";
      })
      .addCase(fetchLoansByUser.fulfilled, (state, action) => {
        state.status = "succeeded";
        state.items = action.payload;
      })
      .addCase(fetchLoansByUser.rejected, (state, action) => {
        state.status = "failed";
        state.error = action.error.message;
      })
      .addCase(borrowBook.fulfilled, (state, action) => {
        state.items.unshift(action.payload);
      })
      .addCase(borrowBook.pending, (state) => {
        state.error = null;
      })
      .addCase(borrowBook.rejected, (state, action) => {
        state.status = "failed";
        state.error = action.error.message;
      })
      .addCase(returnBook.fulfilled, (state, action) => {
        const returnedLoan = action.payload;

        state.items = state.items.map((loan) =>
          loan.id === returnedLoan.id ? returnedLoan : loan,
        );
      })
      .addCase(fetchAdminLoans.pending, (state, action) => {
        state.adminStatus = "loading";
        state.adminError = null;
        state.adminFilter = action.meta.arg;
      })
      .addCase(fetchAdminLoans.fulfilled, (state, action) => {
        state.adminStatus = "succeeded";
        state.adminItems = action.payload;
        state.adminError = null;
      })
      .addCase(fetchAdminLoans.rejected, (state, action) => {
        state.adminStatus = "failed";
        state.adminError = action.error.message;
      });
  },
});

export default loansSlice.reducer;
