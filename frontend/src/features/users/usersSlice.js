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

export const fetchUsers = createAsyncThunk(
  "users/fetchUsers",
  async ({ search = "", page = 0, size = 5 } = {}) => {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size),
    });

    if (search.trim()) {
      params.set("search", search.trim());
    }

    const response = await fetch(`${API_BASE_URL}/users?${params.toString()}`);

    if (!response.ok) {
      const message = await getErrorMessage(response, "Failed to fetch users");
      throw new Error(message);
    }

    return await response.json();
  },
);

export const createUser = createAsyncThunk(
  "users/createUser",
  async (userData) => {
    const response = await fetch(`${API_BASE_URL}/users`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(userData),
    });

    if (!response.ok) {
      const message = await getErrorMessage(response, "Failed to create user");
      throw new Error(message);
    }

    return await response.json();
  },
);

export const updateUser = createAsyncThunk(
  "users/updateUser",
  async ({ userId, name }) => {
    const response = await fetch(`${API_BASE_URL}/users/${userId}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ name }),
    });

    if (!response.ok) {
      const message = await getErrorMessage(response, "Failed to update user");
      throw new Error(message);
    }
    return await response.json();
  },
);

export const deleteUser = createAsyncThunk(
  "users/deleteUser",
  async (userId) => {
    const response = await fetch(`${API_BASE_URL}/users/${userId}`, {
      method: "DELETE",
    });

    if (!response.ok) {
      const message = await getErrorMessage(response, "Failed to delete user");
      throw new Error(message);
    }

    return userId;
  },
);

const usersSlice = createSlice({
  name: "users",
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
  },
  reducers: {
    clearUserActionError: (state) => {
      state.actionError = null;
      state.actionStatus = "idle";
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch users
      .addCase(fetchUsers.pending, (state) => {
        state.status = "loading";
        state.error = null;
      })
      .addCase(fetchUsers.fulfilled, (state, action) => {
        state.status = "succeeded";
        state.items = action.payload.content;
        state.page = action.payload.number;
        state.size = action.payload.size;
        state.totalPages = action.payload.totalPages;
        state.totalElements = action.payload.totalElements;
        state.error = null;
      })
      .addCase(fetchUsers.rejected, (state, action) => {
        state.status = "failed";
        state.error = action.error.message;
      })

      // Create user
      .addCase(createUser.pending, (state) => {
        state.actionStatus = "loading";
        state.actionError = null;
      })
      .addCase(createUser.fulfilled, (state) => {
        state.actionStatus = "succeeded";
        state.actionError = null;
      })
      .addCase(createUser.rejected, (state, action) => {
        state.actionStatus = "failed";
        state.actionError = action.error.message;
      })

      // Update user
      .addCase(updateUser.pending, (state) => {
        state.actionStatus = "loading";
        state.actionError = null;
      })
      .addCase(updateUser.fulfilled, (state, action) => {
        state.actionStatus = "succeeded";
        state.actionError = null;

        state.items = state.items.map((user) =>
          user.id === action.payload.id ? action.payload : user,
        );
      })
      .addCase(updateUser.rejected, (state, action) => {
        state.actionStatus = "failed";
        state.actionError = action.error.message;
      })

      // Soft delete user
      .addCase(deleteUser.pending, (state) => {
        state.actionStatus = "loading";
        state.actionError = null;
      })
      .addCase(deleteUser.fulfilled, (state, action) => {
        state.actionStatus = "succeeded";
        state.actionError = null;

        state.items = state.items.filter(
          (user) => user.id !== action.payload,
        );
      })
      .addCase(deleteUser.rejected, (state, action) => {
        state.actionStatus = "failed";
        state.actionError = action.error.message;
      });
  },
});

export const {clearUserActionError} = usersSlice.actions;

export default usersSlice.reducer;
